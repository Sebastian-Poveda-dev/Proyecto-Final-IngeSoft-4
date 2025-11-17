package com.mio.graph;

import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.LineStop;
import com.mio.model.Arco;

import java.util.*;

/**
 * Construye y gestiona el grafo de paradas y arcos del sistema MIO
 */
public class TransportGraph {
    private Map<Integer, Line> lines;
    private Map<Integer, Stop> stops;
    private List<LineStop> lineStops;
    
    // Arcos organizados por línea y orientación
    private Map<Integer, Map<Integer, List<Arco>>> arcosPorLineaYOrientacion;
    
    public TransportGraph(Map<Integer, Line> lines, Map<Integer, Stop> stops, List<LineStop> lineStops) {
        this.lines = lines;
        this.stops = stops;
        this.lineStops = lineStops;
        this.arcosPorLineaYOrientacion = new HashMap<>();
        
        construirGrafo();
    }
    
    /**
     * Construye el grafo creando arcos entre paradas consecutivas
     * Se agrupan por línea, orientación Y variante para evitar duplicados
     */
    private void construirGrafo() {
        // Organizar lineStops por lineId, orientation y lineVariant
        Map<Integer, Map<Integer, Map<Integer, List<LineStop>>>> lineStopsPorLinea = new HashMap<>();
        
        for (LineStop ls : lineStops) {
            lineStopsPorLinea
                .computeIfAbsent(ls.getLineId(), k -> new HashMap<>())
                .computeIfAbsent(ls.getOrientation(), k -> new HashMap<>())
                .computeIfAbsent(ls.getLineVariant(), k -> new ArrayList<>())
                .add(ls);
        }
        
        // Para cada línea y orientación, crear arcos entre paradas consecutivas de TODAS las variantes
        for (Map.Entry<Integer, Map<Integer, Map<Integer, List<LineStop>>>> entry : lineStopsPorLinea.entrySet()) {
            int lineId = entry.getKey();
            Map<Integer, Map<Integer, List<LineStop>>> orientaciones = entry.getValue();
            
            arcosPorLineaYOrientacion.put(lineId, new HashMap<>());
            
            for (Map.Entry<Integer, Map<Integer, List<LineStop>>> orientEntry : orientaciones.entrySet()) {
                int orientation = orientEntry.getKey();
                Map<Integer, List<LineStop>> variantes = orientEntry.getValue();
                
                // Usaremos un Set para evitar arcos duplicados
                Set<String> arcosUnicos = new HashSet<>();
                List<Arco> arcos = new ArrayList<>();
                
                // Procesar cada variante de la ruta
                for (Map.Entry<Integer, List<LineStop>> variantEntry : variantes.entrySet()) {
                    List<LineStop> paradas = variantEntry.getValue();
                    
                    // Ordenar por secuencia
                    Collections.sort(paradas);
                    
                    // Crear arcos solo entre paradas CONSECUTIVAS
                    for (int i = 0; i < paradas.size() - 1; i++) {
                        LineStop current = paradas.get(i);
                        LineStop next = paradas.get(i + 1);
                        
                        Stop stopOrigen = stops.get(current.getStopId());
                        Stop stopDestino = stops.get(next.getStopId());
                        
                        // Solo crear arco si:
                        // 1. Ambas paradas existen
                        // 2. Son paradas DIFERENTES (evitar autoloops)
                        // 3. No se ha creado ya este arco
                        if (stopOrigen != null && stopDestino != null && 
                            current.getStopId() != next.getStopId()) {
                            
                            String arcoKey = stopOrigen.getStopId() + "->" + stopDestino.getStopId();
                            
                            if (!arcosUnicos.contains(arcoKey)) {
                                Arco arco = new Arco(stopOrigen, stopDestino, lineId, orientation, 
                                                    current.getStopSequence());
                                arcos.add(arco);
                                arcosUnicos.add(arcoKey);
                            }
                        }
                    }
                }
                
                arcosPorLineaYOrientacion.get(lineId).put(orientation, arcos);
            }
        }
    }
    
    /**
     * Muestra el grafo completo en consola, organizado por ruta y orientación
     */
    public void mostrarGrafo() {
        // Obtener todas las líneas ordenadas por ID
        List<Integer> lineIds = new ArrayList<>(arcosPorLineaYOrientacion.keySet());
        Collections.sort(lineIds);
        
        System.out.println("========================================");
        System.out.println("GRAFO DEL SISTEMA MIO - CALI, COLOMBIA");
        System.out.println("========================================\n");
        
        for (int lineId : lineIds) {
            Line line = lines.get(lineId);
            
            if (line != null) {
                System.out.println("================================================================");
                System.out.println(" RUTA: " + line.getShortName() + " - " + line.getDescription());
                System.out.println(" Line ID: " + lineId);
                System.out.println("================================================================");
                
                Map<Integer, List<Arco>> orientaciones = arcosPorLineaYOrientacion.get(lineId);
                
                // Mostrar orientación IDA (0)
                if (orientaciones.containsKey(0)) {
                    List<Arco> arcosIda = orientaciones.get(0);
                    System.out.println("\n--- SENTIDO IDA (Orientation: 0) ---");
                    System.out.println("  | Total de arcos: " + arcosIda.size());
                    System.out.println("  -------------------------------------");
                    
                    for (Arco arco : arcosIda) {
                        System.out.println(arco);
                    }
                }
                
                // Mostrar orientación VUELTA (1)
                if (orientaciones.containsKey(1)) {
                    List<Arco> arcosVuelta = orientaciones.get(1);
                    System.out.println("\n---- SENTIDO VUELTA (Orientation: 1) ---");
                    System.out.println("  | Total de arcos: " + arcosVuelta.size());
                    System.out.println("  -----------------------------------------");
                    
                    for (Arco arco : arcosVuelta) {
                        System.out.println(arco);
                    }
                }
                
                System.out.println("\n");
            }
        }
        
        // Resumen estadístico
        mostrarEstadisticas();
    }
    
    /**
     * Muestra estadísticas generales del grafo
     */
    private void mostrarEstadisticas() {
        int totalLineas = arcosPorLineaYOrientacion.size();
        int totalArcos = 0;
        int totalArcosIda = 0;
        int totalArcosVuelta = 0;
        
        for (Map<Integer, List<Arco>> orientaciones : arcosPorLineaYOrientacion.values()) {
            for (Map.Entry<Integer, List<Arco>> entry : orientaciones.entrySet()) {
                int count = entry.getValue().size();
                totalArcos += count;
                
                if (entry.getKey() == 0) {
                    totalArcosIda += count;
                } else {
                    totalArcosVuelta += count;
                }
            }
        }
        
        System.out.println("========================================");
        System.out.println("ESTADISTICAS DEL GRAFO");
        System.out.println("========================================");
        System.out.println("Total de lineas/rutas: " + totalLineas);
        System.out.println("Total de paradas: " + stops.size());
        System.out.println("Total de arcos: " + totalArcos);
        System.out.println("  - Arcos en sentido IDA: " + totalArcosIda);
        System.out.println("  - Arcos en sentido VUELTA: " + totalArcosVuelta);
        System.out.println("========================================");
    }
    
    /**
     * Obtiene los arcos de una línea específica
     */
    public Map<Integer, List<Arco>> getArcosPorLinea(int lineId) {
        return arcosPorLineaYOrientacion.get(lineId);
    }
    
    /**
     * Obtiene todas las líneas
     */
    public Map<Integer, Line> getLines() {
        return lines;
    }
    
    /**
     * Obtiene todas las paradas
     */
    public Map<Integer, Stop> getStops() {
        return stops;
    }
}
