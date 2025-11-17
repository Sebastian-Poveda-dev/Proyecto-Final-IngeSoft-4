package com.mio;

import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.LineStop;
import com.mio.graph.TransportGraph;
import com.mio.util.CSVReader;
import com.mio.visualization.GraphVisualizer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Programa de ejemplo para generar visualizaciones automáticamente
 */
public class GenerarVisualizacion {
    
    public static void main(String[] args) {
        try {
            System.out.println("Cargando datos del Sistema MIO...\n");
            
            // Leer datos
            Map<Integer, Line> lines = CSVReader.readLines("data/lines-241.csv");
            Map<Integer, Stop> stops = CSVReader.readStops("data/stops-241.csv");
            List<LineStop> lineStops = CSVReader.readLineStops("data/linestops-241.csv");
            
            System.out.println("Construyendo grafo...\n");
            TransportGraph graph = new TransportGraph(lines, stops, lineStops);
            
            // Crear visualizador
            GraphVisualizer visualizer = new GraphVisualizer(graph);
            
            // Generar visualización del grafo completo
            System.out.println("Generando visualización del grafo completo...");
            System.out.println("(esto puede tardar algunos segundos)\n");
            
            visualizer.visualizarGrafoCompleto("output/grafo_completo_MIO.jpg");
            
            System.out.println("\nVisualización generada exitosamente!");
            System.out.println("Archivo: output/grafo_completo_MIO.jpg");
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
