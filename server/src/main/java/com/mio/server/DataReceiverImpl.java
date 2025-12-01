package com.mio.server;

import MIO.DataReceiverPrx;
import MIO.DatagramStruct;
import com.mio.model.Datagram;
import com.zeroc.Ice.Current;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del servidor ICE que recibe datagramas.
 * Persiste datagramas en archivo para procesamiento posterior por CCOController.
 */
public class DataReceiverImpl implements MIO.DataReceiver {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final DatagramRepository repository;
    private long datagramsReceived = 0;
    private long datagramsRejected = 0;
    
    public DataReceiverImpl(DatagramRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public synchronized boolean sendDatagram(DatagramStruct iceStruct, Current current) {
        try {
            // Convertir estructura ICE a Datagram Java
            Datagram datagram = fromIceStruct(iceStruct);
            
            // Persistir en archivo
            repository.save(datagram);
            
            datagramsReceived++;
            if (datagramsReceived % 1000 == 0) {
                System.out.printf("[DataReceiver] Recibidos: %d | Persistidos: %d\n",
                    datagramsReceived, repository.getTotalSaved());
            }
            return true;
            
        } catch (Exception e) {
            datagramsRejected++;
            System.err.println("[DataReceiver] Error persistiendo datagrama: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean ping(Current current) {
        return true;
    }
    
    /**
     * Convierte DatagramStruct de ICE a Datagram Java.
     */
    private Datagram fromIceStruct(DatagramStruct ice) {
        LocalDateTime registerDate = parseDateTime(ice.registerDate);
        LocalDateTime datagramDate = parseDateTime(ice.datagramDate);
        
        return new Datagram(
            ice.eventType,
            registerDate,
            ice.stopId,
            ice.odometer,
            ice.latitude,
            ice.longitude,
            ice.taskId,
            ice.lineId,
            ice.tripId,
            ice.unknown1,
            datagramDate,
            ice.busId
        );
    }
    
    /**
     * Parsea fecha/hora del formato ICE.
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Estadísticas del receiver.
     */
    public synchronized long getReceivedCount() {
        return datagramsReceived;
    }
    
    public synchronized long getRejectedCount() {
        return datagramsRejected;
    }
}
