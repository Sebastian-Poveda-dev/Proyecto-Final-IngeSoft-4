package com.mio.server;

import MIO.*;
import com.zeroc.Ice.Current;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphQueryServiceImpl - Implementación del servicio de consultas al grafo.
 * 
 * Usa el Aggregator para responder consultas sobre velocidades promedio
 * de arcos y líneas del sistema de transporte.
 */
public class GraphQueryServiceImpl implements GraphQueryService {
    
    private final Aggregator aggregator;
    
    public GraphQueryServiceImpl(Aggregator aggregator) {
        this.aggregator = aggregator;
    }
    
    @Override
    public double getVelocidadPromedioArco(String arcoId, Current current) {
        double velocidad = aggregator.getVelocidadPromedio(arcoId);
        return velocidad > 0 ? velocidad : -1;
    }
    
    @Override
    public ArcoInfo getArcoInfo(String arcoId, Current current) {
        ArcoInfo info = new ArcoInfo();
        info.arcoId = arcoId;
        
        // Parsear arcoId: "lineId-stopOrigen-stopDestino"
        String[] parts = arcoId.split("-");
        if (parts.length >= 3) {
            try {
                info.lineId = Integer.parseInt(parts[0]);
                info.stopOrigen = Integer.parseInt(parts[1]);
                info.stopDestino = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                info.lineId = 0;
                info.stopOrigen = 0;
                info.stopDestino = 0;
            }
        }
        
        info.velocidadPromedio = aggregator.getVelocidadPromedio(arcoId);
        info.observaciones = aggregator.getConteo(arcoId);
        
        return info;
    }
    
    @Override
    public ArcoInfo[] getVelocidadesPorLinea(int lineId, Current current) {
        List<ArcoInfo> arcos = new ArrayList<>();
        String prefix = lineId + "-";
        
        for (String arcoId : aggregator.getArcosConResultados()) {
            if (arcoId.startsWith(prefix)) {
                arcos.add(getArcoInfo(arcoId, current));
            }
        }
        
        // Ordenar por velocidad descendente
        arcos.sort((a, b) -> Double.compare(b.velocidadPromedio, a.velocidadPromedio));
        
        return arcos.toArray(new ArcoInfo[0]);
    }
    
    @Override
    public double getVelocidadPromedioLinea(int lineId, Current current) {
        ArcoInfo[] arcos = getVelocidadesPorLinea(lineId, current);
        
        if (arcos.length == 0) {
            return -1;
        }
        
        double sumaVelocidades = 0;
        int totalObservaciones = 0;
        
        for (ArcoInfo arco : arcos) {
            sumaVelocidades += arco.velocidadPromedio * arco.observaciones;
            totalObservaciones += arco.observaciones;
        }
        
        return totalObservaciones > 0 ? sumaVelocidades / totalObservaciones : 0;
    }
    
    @Override
    public LineaInfo[] getTodasLasLineas(Current current) {
        // Agrupar arcos por línea
        Map<Integer, List<ArcoInfo>> arcosPorLinea = new HashMap<>();
        
        for (String arcoId : aggregator.getArcosConResultados()) {
            String[] parts = arcoId.split("-");
            if (parts.length >= 1) {
                try {
                    int lineId = Integer.parseInt(parts[0]);
                    ArcoInfo info = getArcoInfo(arcoId, current);
                    arcosPorLinea.computeIfAbsent(lineId, k -> new ArrayList<>()).add(info);
                } catch (NumberFormatException e) {
                    // Ignorar arcos con formato inválido
                }
            }
        }
        
        // Crear LineaInfo para cada línea
        List<LineaInfo> lineas = new ArrayList<>();
        
        for (Map.Entry<Integer, List<ArcoInfo>> entry : arcosPorLinea.entrySet()) {
            LineaInfo linea = new LineaInfo();
            linea.lineId = entry.getKey();
            linea.nombre = "Línea " + entry.getKey();
            linea.cantidadArcos = entry.getValue().size();
            
            // Calcular velocidad promedio ponderada
            double sumaVel = 0;
            int totalObs = 0;
            for (ArcoInfo arco : entry.getValue()) {
                sumaVel += arco.velocidadPromedio * arco.observaciones;
                totalObs += arco.observaciones;
            }
            linea.velocidadPromedio = totalObs > 0 ? sumaVel / totalObs : 0;
            
            lineas.add(linea);
        }
        
        // Ordenar por lineId
        lineas.sort(Comparator.comparingInt(l -> l.lineId));
        
        return lineas.toArray(new LineaInfo[0]);
    }
    
    @Override
    public ArcoInfo[] getArcosMasLentos(int limite, Current current) {
        List<ArcoInfo> todos = new ArrayList<>();
        
        for (String arcoId : aggregator.getArcosConResultados()) {
            ArcoInfo info = getArcoInfo(arcoId, current);
            if (info.velocidadPromedio > 0) {
                todos.add(info);
            }
        }
        
        // Ordenar por velocidad ascendente (más lentos primero)
        todos.sort(Comparator.comparingDouble(a -> a.velocidadPromedio));
        
        // Limitar resultados
        return todos.stream()
            .limit(limite)
            .toArray(ArcoInfo[]::new);
    }
    
    @Override
    public ArcoInfo[] getArcosMasRapidos(int limite, Current current) {
        List<ArcoInfo> todos = new ArrayList<>();
        
        for (String arcoId : aggregator.getArcosConResultados()) {
            ArcoInfo info = getArcoInfo(arcoId, current);
            if (info.velocidadPromedio > 0) {
                todos.add(info);
            }
        }
        
        // Ordenar por velocidad descendente (más rápidos primero)
        todos.sort((a, b) -> Double.compare(b.velocidadPromedio, a.velocidadPromedio));
        
        // Limitar resultados
        return todos.stream()
            .limit(limite)
            .toArray(ArcoInfo[]::new);
    }
    
    @Override
    public String getEstadisticasGenerales(Current current) {
        StringBuilder sb = new StringBuilder();
        
        LineaInfo[] lineas = getTodasLasLineas(current);
        int totalArcos = aggregator.getArcoCount();
        long totalResultados = aggregator.getTotalResultadosRecibidos();
        long totalLotes = aggregator.getTotalLotesProcesados();
        
        // Calcular velocidad promedio global
        double sumaGlobal = 0;
        int obsGlobal = 0;
        for (String arcoId : aggregator.getArcosConResultados()) {
            double suma = aggregator.getSumaVelocidades(arcoId);
            int conteo = aggregator.getConteo(arcoId);
            sumaGlobal += suma;
            obsGlobal += conteo;
        }
        double velPromedioGlobal = obsGlobal > 0 ? sumaGlobal / obsGlobal : 0;
        
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║              ESTADÍSTICAS DEL SISTEMA MIO                    ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Total líneas:           %-35d ║\n", lineas.length));
        sb.append(String.format("║  Total arcos:            %-35d ║\n", totalArcos));
        sb.append(String.format("║  Total observaciones:    %-35d ║\n", obsGlobal));
        sb.append(String.format("║  Lotes procesados:       %-35d ║\n", totalLotes));
        sb.append(String.format("║  Velocidad promedio:     %-31.2f km/h ║\n", velPromedioGlobal));
        sb.append("╚══════════════════════════════════════════════════════════════╝");
        
        return sb.toString();
    }
    
    @Override
    public boolean ping(Current current) {
        return true;
    }
}
