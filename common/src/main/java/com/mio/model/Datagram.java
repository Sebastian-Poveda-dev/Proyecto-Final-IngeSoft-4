package com.mio.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un datagrama del dataset datagrams4streaming.
 * Cada datagrama contiene información de un evento de un bus en el sistema MIO.
 * 
 * Campos del CSV (separados por coma):
 * 1. eventType
 * 2. registerdate
 * 3. stopId
 * 4. odometer
 * 5. latitude
 * 6. longitude
 * 7. taskId
 * 8. lineId
 * 9. tripId
 * 10. unknown1
 * 11. datagramDate
 * 12. busId
 */
public class Datagram {
    private String eventType;
    private LocalDateTime registerDate;
    private int stopId;
    private double odometer;
    private double latitude;
    private double longitude;
    private int taskId;
    private int lineId;
    private int tripId;
    private String unknown1;
    private LocalDateTime datagramDate;
    private int busId;
    
    // Formateador para parsear fechas del CSV
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Datagram(String eventType, LocalDateTime registerDate, int stopId, 
                    double odometer, double latitude, double longitude,
                    int taskId, int lineId, int tripId, String unknown1,
                    LocalDateTime datagramDate, int busId) {
        this.eventType = eventType;
        this.registerDate = registerDate;
        this.stopId = stopId;
        this.odometer = odometer;
        this.latitude = latitude;
        this.longitude = longitude;
        this.taskId = taskId;
        this.lineId = lineId;
        this.tripId = tripId;
        this.unknown1 = unknown1;
        this.datagramDate = datagramDate;
        this.busId = busId;
    }

    /**
     * Parsea una línea CSV del dataset datagrams4streaming y crea un objeto Datagram.
     * 
     * @param csvLine línea del CSV con formato: eventType,registerdate,stopId,odometer,latitude,longitude,taskId,lineId,tripId,unknown1,datagramDate,busId
     * @return objeto Datagram parseado
     * @throws IllegalArgumentException si la línea no tiene el formato esperado
     */
    public static Datagram fromCsvLine(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Línea CSV vacía o nula");
        }
        
        String[] fields = csvLine.split(",");
        
        if (fields.length < 12) {
            throw new IllegalArgumentException(
                "Línea CSV no tiene suficientes campos (esperados: 12, encontrados: " + 
                fields.length + ")");
        }
        
        try {
            String eventType = fields[0].trim();
            LocalDateTime registerDate = parseDateTime(fields[1].trim());
            int stopId = Integer.parseInt(fields[2].trim());
            double odometer = Double.parseDouble(fields[3].trim());
            double latitude = Double.parseDouble(fields[4].trim());
            double longitude = Double.parseDouble(fields[5].trim());
            int taskId = Integer.parseInt(fields[6].trim());
            int lineId = Integer.parseInt(fields[7].trim());
            int tripId = Integer.parseInt(fields[8].trim());
            String unknown1 = fields[9].trim();
            LocalDateTime datagramDate = parseDateTime(fields[10].trim());
            int busId = Integer.parseInt(fields[11].trim());
            
            return new Datagram(eventType, registerDate, stopId, odometer, 
                              latitude, longitude, taskId, lineId, tripId,
                              unknown1, datagramDate, busId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Error parseando línea CSV: " + csvLine + " - " + e.getMessage(), e);
        }
    }
    
    /**
     * Parsea una fecha/hora del formato del CSV
     */
    private static LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            // Si falla el formato estándar, intentar sin segundos
            try {
                return LocalDateTime.parse(dateStr, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    // Getters
    
    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getRegisterDate() {
        return registerDate;
    }

    public int getStopId() {
        return stopId;
    }

    public double getOdometer() {
        return odometer;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getLineId() {
        return lineId;
    }

    public int getTripId() {
        return tripId;
    }

    public String getUnknown1() {
        return unknown1;
    }

    public LocalDateTime getDatagramDate() {
        return datagramDate;
    }

    public int getBusId() {
        return busId;
    }
    
    /**
     * Genera un identificador para mapear este datagrama a un arco.
     * Usa lineId + stopId como clave de búsqueda.
     * @return String en formato "lineId:stopId"
     */
    public String getArcoKey() {
        return String.format("%d:%d", lineId, stopId);
    }

    @Override
    public String toString() {
        return String.format("Datagram{busId=%d, lineId=%d, tripId=%d, stopId=%d, " +
                           "odometer=%.2f, lat=%.6f, lon=%.6f, date=%s}",
                           busId, lineId, tripId, stopId, odometer, 
                           latitude, longitude, datagramDate);
    }
    
    /**
     * Formato CSV para serialización
     */
    public String toCsvLine() {
        return String.format("%s,%s,%d,%.2f,%.6f,%.6f,%d,%d,%d,%s,%s,%d",
                           eventType,
                           registerDate != null ? registerDate.format(DATE_FORMATTER) : "",
                           stopId, odometer, latitude, longitude,
                           taskId, lineId, tripId, unknown1,
                           datagramDate != null ? datagramDate.format(DATE_FORMATTER) : "",
                           busId);
    }
}
