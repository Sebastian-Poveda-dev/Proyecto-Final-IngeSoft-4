package com.mio.server;

import com.mio.model.Datagram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Repositorio para persistir datagramas recibidos del Bus.
 * Guarda en archivo CSV para procesamiento posterior por CCOController.
 */
public class DatagramRepository {
    private final String filePath;
    private final Lock writeLock;
    private long totalSaved;
    
    public DatagramRepository(String filePath) {
        this.filePath = filePath;
        this.writeLock = new ReentrantLock();
        this.totalSaved = 0;
        
        // Crear directorio si no existe
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        // Crear archivo con header si no existe
        if (!file.exists()) {
            try {
                writeHeader();
            } catch (IOException e) {
                System.err.println("[DatagramRepository] Error creando archivo: " + e.getMessage());
            }
        }
    }
    
    /**
     * Escribe el header del CSV.
     */
    private void writeHeader() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write("eventType,registerDate,stopId,odometer,latitude,longitude,taskId,lineId,tripId,unknown1,datagramDate,busId");
            writer.newLine();
        }
    }
    
    /**
     * Guarda un datagrama en el archivo CSV (thread-safe).
     */
    public void save(Datagram datagram) throws IOException {
        writeLock.lock();
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                writer.write(datagram.toCsvLine());
                writer.newLine();
                totalSaved++;
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Guarda múltiples datagramas en batch (thread-safe).
     */
    public void saveAll(List<Datagram> datagrams) throws IOException {
        writeLock.lock();
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                for (Datagram datagram : datagrams) {
                    writer.write(datagram.toCsvLine());
                    writer.newLine();
                }
                totalSaved += datagrams.size();
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Cuenta total de datagramas guardados en esta sesión.
     */
    public long getTotalSaved() {
        return totalSaved;
    }
    
    /**
     * Cuenta líneas en el archivo (excluyendo header).
     */
    public long countInFile() {
        try {
            long count = Files.lines(Paths.get(filePath)).count();
            return count > 0 ? count - 1 : 0; // Restar header
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Path del archivo de persistencia.
     */
    public String getFilePath() {
        return filePath;
    }
}
