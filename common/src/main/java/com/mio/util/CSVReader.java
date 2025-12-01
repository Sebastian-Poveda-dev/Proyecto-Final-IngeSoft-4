package com.mio.util;

import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.LineStop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase para leer los archivos CSV del sistema MIO
 */
public class CSVReader {
    
    /**
     * Lee el archivo de líneas/rutas
     */
    public static Map<Integer, Line> readLines(String filePath) throws IOException {
        Map<Integer, Line> lines = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // Saltar encabezado
            
            while ((line = br.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                
                if (fields.length >= 6) {
                    int lineId = Integer.parseInt(fields[0]);
                    int planVersionId = Integer.parseInt(fields[1]);
                    String shortName = fields[2];
                    String description = fields[3];
                    String activationDate = fields[5];
                    
                    Line lineObj = new Line(lineId, planVersionId, shortName, description, activationDate);
                    lines.put(lineId, lineObj);
                }
            }
        }
        
        return lines;
    }
    
    /**
     * Lee el archivo de paradas
     */
    public static Map<Integer, Stop> readStops(String filePath) throws IOException {
        Map<Integer, Stop> stops = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // Saltar encabezado
            
            while ((line = br.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                
                if (fields.length >= 8) {
                    int stopId = Integer.parseInt(fields[0]);
                    int planVersionId = Integer.parseInt(fields[1]);
                    String shortName = fields[2];
                    String longName = fields[3];
                    double gpsX = Double.parseDouble(fields[4]);
                    double gpsY = Double.parseDouble(fields[5]);
                    double decimalLong = Double.parseDouble(fields[6]);
                    double decimalLat = Double.parseDouble(fields[7]);
                    
                    Stop stop = new Stop(stopId, planVersionId, shortName, longName, 
                                        gpsX, gpsY, decimalLong, decimalLat);
                    stops.put(stopId, stop);
                }
            }
        }
        
        return stops;
    }
    
    /**
     * Lee el archivo de línea-paradas (relación entre rutas y paradas)
     */
    public static List<LineStop> readLineStops(String filePath) throws IOException {
        List<LineStop> lineStops = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // Saltar encabezado
            
            while ((line = br.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                
                if (fields.length >= 7) {
                    int lineStopId = Integer.parseInt(fields[0]);
                    int stopSequence = Integer.parseInt(fields[1]);
                    int orientation = Integer.parseInt(fields[2]);
                    int lineId = Integer.parseInt(fields[3]);
                    int stopId = Integer.parseInt(fields[4]);
                    int planVersionId = Integer.parseInt(fields[5]);
                    int lineVariant = Integer.parseInt(fields[6]);
                    
                    LineStop lineStop = new LineStop(lineStopId, stopSequence, orientation, 
                                                     lineId, stopId, planVersionId, lineVariant);
                    lineStops.add(lineStop);
                }
            }
        }
        
        return lineStops;
    }
    
    /**
     * Parsea una línea CSV considerando comillas
     */
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        result.add(current.toString().trim());
        
        return result.toArray(new String[0]);
    }
}
