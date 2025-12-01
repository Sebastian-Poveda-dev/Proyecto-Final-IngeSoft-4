package com.mio;

import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.LineStop;
import com.mio.graph.TransportGraph;
import com.mio.util.CSVReader;
import com.mio.visualization.GraphVisualizer;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Clase principal para ejecutar el proyecto del Sistema MIO
 * Lee los archivos CSV, construye el grafo y muestra los arcos por ruta
 */
public class Main {
    
    public static void main(String[] args) {
        // Configurar codificación UTF-8 para la consola
        configurarUTF8();
        
        try {
            System.out.println("Iniciando lectura de archivos CSV del Sistema MIO...\n");
            
            // Rutas de los archivos CSV
            String linesPath = "data/lines-241.csv";
            String stopsPath = "data/stops-241.csv";
            String lineStopsPath = "data/linestops-241.csv";
            
            // Leer datos de los archivos CSV
            System.out.println("Leyendo archivo de líneas/rutas...");
            Map<Integer, Line> lines = CSVReader.readLines(linesPath);
            System.out.println("--" + lines.size() + " líneas cargadas");
            
            System.out.println("Leyendo archivo de paradas...");
            Map<Integer, Stop> stops = CSVReader.readStops(stopsPath);
            System.out.println("--" + stops.size() + " paradas cargadas");
            
            System.out.println("Leyendo archivo de línea-paradas...");
            List<LineStop> lineStops = CSVReader.readLineStops(lineStopsPath);
            System.out.println("--" + lineStops.size() + " relaciones línea-parada cargadas");
            
            System.out.println("\nConstruyendo grafo del sistema de transporte...\n");
            
            // Construir el grafo
            TransportGraph graph = new TransportGraph(lines, stops, lineStops);
            
            // Mostrar el grafo completo con arcos ordenados por secuencia
            graph.mostrarGrafo();
            
            // Preguntar si desea generar visualización
            System.out.println("\n¿Desea generar visualizaciones del grafo? (s/n): ");
            Scanner scanner = new Scanner(System.in);
            String respuesta = scanner.nextLine().trim().toLowerCase();
            
            if (respuesta.equals("s") || respuesta.equals("si") || respuesta.equals("sí")) {
                GraphVisualizer visualizer = new GraphVisualizer(graph);
                
                System.out.println("\nOpciones de visualización:");
                System.out.println("1. Visualizar grafo completo");
                System.out.println("2. Visualizar una línea específica");
                System.out.println("3. Visualizar varias líneas de ejemplo");
                System.out.print("Seleccione una opción (1-3): ");
                
                String opcion = scanner.nextLine().trim();
                
                switch (opcion) {
                    case "1":
                        System.out.println("\nGenerando visualización del grafo completo...");
                        visualizer.visualizarGrafoCompleto("output/grafo_completo.jpg");
                        break;
                        
                    case "2":
                        System.out.print("Ingrese el ID de la línea a visualizar: ");
                        int lineId = Integer.parseInt(scanner.nextLine().trim());
                        Line line = lines.get(lineId);
                        if (line != null) {
                            String filename = "output/linea_" + line.getShortName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                            System.out.println("\nGenerando visualización de línea " + line.getShortName() + "...");
                            visualizer.visualizarLinea(lineId, filename);
                        } else {
                            System.out.println("Línea no encontrada.");
                        }
                        break;
                        
                    case "3":
                        System.out.println("\nGenerando visualizaciones de líneas de ejemplo...");
                        // Buscar algunas líneas para visualizar
                        int count = 0;
                        for (Map.Entry<Integer, Line> entry : lines.entrySet()) {
                            if (count >= 5) break;
                            int id = entry.getKey();
                            Line l = entry.getValue();
                            String filename = "output/linea_" + l.getShortName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                            visualizer.visualizarLinea(id, filename);
                            count++;
                        }
                        System.out.println("✓ Generadas " + count + " visualizaciones de ejemplo");
                        break;
                        
                    default:
                        System.out.println("Opción no válida.");
                }
            }
            
            System.out.println("\n✓ Proceso completado exitosamente");
            
        } catch (IOException e) {
            System.err.println("Error al leer los archivos CSV: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configura la codificación UTF-8 para la salida de consola
     */
    private static void configurarUTF8() {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("No se pudo configurar UTF-8: " + e.getMessage());
        }
    }
}
