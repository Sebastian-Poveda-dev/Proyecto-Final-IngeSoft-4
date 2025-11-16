package com.mio;

import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.LineStop;
import com.mio.graph.TransportGraph;
import com.mio.util.CSVReader;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
