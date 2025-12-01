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
    
    // Arcos organizados por línea, orientación y variante
    // lineId -> orientation -> variant -> List<Arco>
    private Map<Integer, Map<Integer, Map<Integer, List<Arco>>>> arcosPorLineaOrientacionVariante;
    
    public TransportGraph(Map<Integer, Line> lines, Map<Integer, Stop> stops, List<LineStop> lineStops) {
        this.lines = lines;
        this.stops = stops;
        this.lineStops = lineStops;
        this.arcosPorLineaOrientacionVariante = new HashMap<>();
        
        construirGrafo();
    }
    
    /**
     * Construye el grafo creando arcos entre paradas consecutivas
     * Mantiene las variantes de ruta separadas
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
        
        // Para cada línea, orientación y variante, crear arcos entre paradas consecutivas
        for (Map.Entry<Integer, Map<Integer, Map<Integer, List<LineStop>>>> entry : lineStopsPorLinea.entrySet()) {
            int lineId = entry.getKey();
            Map<Integer, Map<Integer, List<LineStop>>> orientaciones = entry.getValue();
            
            arcosPorLineaOrientacionVariante.put(lineId, new HashMap<>());
            
            for (Map.Entry<Integer, Map<Integer, List<LineStop>>> orientEntry : orientaciones.entrySet()) {
                int orientation = orientEntry.getKey();
                Map<Integer, List<LineStop>> variantes = orientEntry.getValue();
                
                arcosPorLineaOrientacionVariante.get(lineId).put(orientation, new HashMap<>());
                
                // Procesar cada variante de la ruta POR SEPARADO
                for (Map.Entry<Integer, List<LineStop>> variantEntry : variantes.entrySet()) {
                    int variant = variantEntry.getKey();
                    List<LineStop> paradas = variantEntry.getValue();
                    
                    // Ordenar por secuencia
                    Collections.sort(paradas);
                    
                    List<Arco> arcos = new ArrayList<>();
                    
                    // Crear arcos solo entre paradas CONSECUTIVAS
                    for (int i = 0; i < paradas.size() - 1; i++) {
                        LineStop current = paradas.get(i);
                        LineStop next = paradas.get(i + 1);
                        
                        Stop stopOrigen = stops.get(current.getStopId());
                        Stop stopDestino = stops.get(next.getStopId());
                        
                        // Solo crear arco si:
                        // 1. Ambas paradas existen
                        // 2. Son paradas DIFERENTES (evitar autoloops)
                        if (stopOrigen != null && stopDestino != null && 
                            current.getStopId() != next.getStopId()) {
                            
                            Arco arco = new Arco(stopOrigen, stopDestino, lineId, orientation, 
                                                current.getStopSequence());
                            arcos.add(arco);
                        }
                    }
                    
                    arcosPorLineaOrientacionVariante.get(lineId).get(orientation).put(variant, arcos);
                }
            }
        }
    }
    
    /**
     * Muestra el grafo completo en consola, organizado por ruta, orientación y variante
     */
    public void mostrarGrafo() {
        // Obtener todas las líneas ordenadas por ID
        List<Integer> lineIds = new ArrayList<>(arcosPorLineaOrientacionVariante.keySet());
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
                
                Map<Integer, Map<Integer, List<Arco>>> orientaciones = arcosPorLineaOrientacionVariante.get(lineId);
                
                // Mostrar orientación IDA (0)
                if (orientaciones.containsKey(0)) {
                    Map<Integer, List<Arco>> variantes = orientaciones.get(0);
                    List<Integer> variantIds = new ArrayList<>(variantes.keySet());
                    Collections.sort(variantIds);
                    
                    for (int variantId : variantIds) {
                        List<Arco> arcosIda = variantes.get(variantId);
                        System.out.println("\n--- SENTIDO IDA (Orientation: 0, Variante: " + variantId + ") ---");
                        System.out.println("  | Total de arcos: " + arcosIda.size());
                        System.out.println("  -------------------------------------");
                        
                        for (Arco arco : arcosIda) {
                            System.out.println(arco);
                        }
                    }
                }
                
                // Mostrar orientación VUELTA (1)
                if (orientaciones.containsKey(1)) {
                    Map<Integer, List<Arco>> variantes = orientaciones.get(1);
                    List<Integer> variantIds = new ArrayList<>(variantes.keySet());
                    Collections.sort(variantIds);
                    
                    for (int variantId : variantIds) {
                        List<Arco> arcosVuelta = variantes.get(variantId);
                        System.out.println("\n---- SENTIDO VUELTA (Orientation: 1, Variante: " + variantId + ") ---");
                        System.out.println("  | Total de arcos: " + arcosVuelta.size());
                        System.out.println("  -----------------------------------------");
                        
                        for (Arco arco : arcosVuelta) {
                            System.out.println(arco);
                        }
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
        int totalLineas = arcosPorLineaOrientacionVariante.size();
        int totalArcos = 0;
        int totalArcosIda = 0;
        int totalArcosVuelta = 0;
        int totalVariantes = 0;
        
        for (Map<Integer, Map<Integer, List<Arco>>> orientaciones : arcosPorLineaOrientacionVariante.values()) {
            for (Map.Entry<Integer, Map<Integer, List<Arco>>> orientEntry : orientaciones.entrySet()) {
                int orientation = orientEntry.getKey();
                Map<Integer, List<Arco>> variantes = orientEntry.getValue();
                
                totalVariantes += variantes.size();
                
                for (List<Arco> arcos : variantes.values()) {
                    int count = arcos.size();
                    totalArcos += count;
                    
                    if (orientation == 0) {
                        totalArcosIda += count;
                    } else {
                        totalArcosVuelta += count;
                    }
                }
            }
        }
        
        System.out.println("========================================");
        System.out.println("ESTADISTICAS DEL GRAFO");
        System.out.println("========================================");
        System.out.println("Total de lineas/rutas: " + totalLineas);
        System.out.println("Total de variantes: " + totalVariantes);
        System.out.println("Total de paradas: " + stops.size());
        System.out.println("Total de arcos: " + totalArcos);
        System.out.println("  - Arcos en sentido IDA: " + totalArcosIda);
        System.out.println("  - Arcos en sentido VUELTA: " + totalArcosVuelta);
        System.out.println("========================================");
    }
    
    /**
     * Obtiene los arcos de una línea específica (con todas sus variantes)
     */
    public Map<Integer, Map<Integer, List<Arco>>> getArcosPorLinea(int lineId) {
        return arcosPorLineaOrientacionVariante.get(lineId);
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
