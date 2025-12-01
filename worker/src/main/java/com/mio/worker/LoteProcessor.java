package com.mio.worker;

import MIO.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * LoteProcessor - Procesa lotes de datagramas usando un ThreadPool.
 * 
 * Divide el lote entre hilos, cada hilo:
 * 1. Mapea Datagram a Arco (lineId + stopId origen → stopId destino)
 * 2. Calcula velocidad instantánea
 * 3. Agrega resultados parciales por arco
 */
public class LoteProcessor {
    
    private final ExecutorService threadPool;
    private final int numThreads;
    
    public LoteProcessor(int numThreads) {
        this.numThreads = numThreads;
        this.threadPool = Executors.newFixedThreadPool(numThreads);
    }
    
    /**
     * Procesa un lote de datagramas y retorna resultados parciales.
     * 
     * @param lote El lote de datagramas a procesar
     * @param workerId ID del worker para identificar los resultados
     * @return ResultadosParciales con velocidades agregadas por arco
     */
    public ResultadosParciales procesarLote(LoteDatagram lote, String workerId) {
        DatagramStruct[] datagramas = lote.datagramas;
        int total = datagramas.length;
        
        // Dividir trabajo entre threads
        int chunkSize = Math.max(1, (total + numThreads - 1) / numThreads);
        List<Future<Map<String, ArcoAcumulador>>> futures = new ArrayList<>();
        
        for (int i = 0; i < numThreads && i * chunkSize < total; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, total);
            
            // Crear tarea para este chunk
            DatagramStruct[] chunk = new DatagramStruct[end - start];
            System.arraycopy(datagramas, start, chunk, 0, end - start);
            
            futures.add(threadPool.submit(new ChunkProcessor(chunk)));
        }
        
        // Combinar resultados de todos los hilos
        Map<String, ArcoAcumulador> resultadosCombinados = new HashMap<>();
        
        for (Future<Map<String, ArcoAcumulador>> future : futures) {
            try {
                Map<String, ArcoAcumulador> parcial = future.get();
                
                // Merge con resultados existentes
                for (Map.Entry<String, ArcoAcumulador> entry : parcial.entrySet()) {
                    resultadosCombinados.merge(entry.getKey(), entry.getValue(), 
                        (a, b) -> {
                            a.sumaVelocidades += b.sumaVelocidades;
                            a.conteo += b.conteo;
                            return a;
                        });
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("[LoteProcessor] Error procesando chunk: " + e.getMessage());
            }
        }
        
        // Convertir a formato ICE
        return crearResultadosParciales(lote.loteId, workerId, resultadosCombinados);
    }
    
    /**
     * Convierte los resultados acumulados a formato ICE.
     */
    private ResultadosParciales crearResultadosParciales(int loteId, String workerId, 
            Map<String, ArcoAcumulador> acumulados) {
        
        ResultadosParciales resultado = new ResultadosParciales();
        resultado.loteId = loteId;
        resultado.workerId = workerId;
        resultado.resultados = new ArcoResult[acumulados.size()];
        
        int i = 0;
        for (Map.Entry<String, ArcoAcumulador> entry : acumulados.entrySet()) {
            ArcoResult ar = new ArcoResult();
            ar.arcoId = entry.getKey();
            ar.sumaVelocidades = entry.getValue().sumaVelocidades;
            ar.conteo = entry.getValue().conteo;
            resultado.resultados[i++] = ar;
        }
        
        return resultado;
    }
    
    /**
     * Detiene el ThreadPool.
     */
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Clase interna para acumular datos por arco.
     */
    static class ArcoAcumulador {
        double sumaVelocidades;
        int conteo;
        
        ArcoAcumulador(double velocidad) {
            this.sumaVelocidades = velocidad;
            this.conteo = 1;
        }
    }
    
    /**
     * Callable que procesa un chunk de datagramas.
     */
    static class ChunkProcessor implements Callable<Map<String, ArcoAcumulador>> {
        
        private final DatagramStruct[] datagramas;
        
        ChunkProcessor(DatagramStruct[] datagramas) {
            this.datagramas = datagramas;
        }
        
        @Override
        public Map<String, ArcoAcumulador> call() {
            Map<String, ArcoAcumulador> resultados = new HashMap<>();
            
            // Agrupar datagramas por bus y línea para calcular velocidades entre paradas
            // Primero ordenamos por tripId (viaje) y fecha para tener secuencia correcta
            Map<String, List<DatagramStruct>> porViaje = new HashMap<>();
            
            for (DatagramStruct d : datagramas) {
                if (d.tripId > 0 && d.lineId > 0 && d.stopId > 0) {
                    String key = d.busId + "-" + d.tripId + "-" + d.lineId;
                    porViaje.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
                }
            }
            
            // Para cada viaje, calcular velocidades entre paradas consecutivas
            for (List<DatagramStruct> viaje : porViaje.values()) {
                if (viaje.size() < 2) continue;
                
                // Ordenar por fecha del datagrama
                viaje.sort((a, b) -> a.datagramDate.compareTo(b.datagramDate));
                
                // Calcular velocidad entre paradas consecutivas
                for (int i = 1; i < viaje.size(); i++) {
                    DatagramStruct prev = viaje.get(i - 1);
                    DatagramStruct curr = viaje.get(i);
                    
                    // Solo si son paradas diferentes
                    if (prev.stopId != curr.stopId) {
                        // Calcular velocidad usando odómetro y tiempo
                        double velocidad = calcularVelocidad(prev, curr);
                        
                        if (velocidad > 0 && velocidad < 120) { // Velocidad válida (km/h)
                            // ID del arco: lineId-stopOrigen-stopDestino
                            String arcoId = String.format("%d-%d-%d", 
                                curr.lineId, prev.stopId, curr.stopId);
                            
                            resultados.merge(arcoId, 
                                new ArcoAcumulador(velocidad),
                                (a, b) -> {
                                    a.sumaVelocidades += b.sumaVelocidades;
                                    a.conteo += b.conteo;
                                    return a;
                                });
                        }
                    }
                }
            }
            
            return resultados;
        }
        
        /**
         * Calcula la velocidad entre dos datagramas consecutivos.
         * Usa odómetro y diferencia de tiempo.
         * 
         * @return velocidad en km/h
         */
        private double calcularVelocidad(DatagramStruct prev, DatagramStruct curr) {
            // Diferencia de odómetro (en metros, convertir a km)
            double distanciaKm = Math.abs(curr.odometer - prev.odometer) / 1000.0;
            
            // Si la distancia es 0 o muy pequeña, usar distancia euclidiana
            if (distanciaKm < 0.01) {
                distanciaKm = calcularDistanciaHaversine(
                    prev.latitude, prev.longitude,
                    curr.latitude, curr.longitude);
            }
            
            // Diferencia de tiempo en horas
            double tiempoHoras = calcularDiferenciaTiempo(prev.datagramDate, curr.datagramDate);
            
            if (tiempoHoras <= 0 || distanciaKm <= 0) {
                return 0;
            }
            
            return distanciaKm / tiempoHoras;
        }
        
        /**
         * Calcula la diferencia de tiempo entre dos fechas en formato String.
         * Formato esperado: "yyyy-MM-dd HH:mm:ss"
         * 
         * @return diferencia en horas
         */
        private double calcularDiferenciaTiempo(String fecha1, String fecha2) {
            try {
                java.time.LocalDateTime dt1 = java.time.LocalDateTime.parse(
                    fecha1.replace(" ", "T"));
                java.time.LocalDateTime dt2 = java.time.LocalDateTime.parse(
                    fecha2.replace(" ", "T"));
                
                long segundos = java.time.Duration.between(dt1, dt2).getSeconds();
                return Math.abs(segundos) / 3600.0;
            } catch (Exception e) {
                return 0;
            }
        }
        
        /**
         * Calcula la distancia Haversine entre dos coordenadas GPS.
         * 
         * @return distancia en km
         */
        private double calcularDistanciaHaversine(double lat1, double lon1, 
                double lat2, double lon2) {
            final double R = 6371; // Radio de la Tierra en km
            
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                       Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                       Math.sin(dLon / 2) * Math.sin(dLon / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            return R * c;
        }
    }
}
