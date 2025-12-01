package com.mio.server;

import com.mio.model.Datagram;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CCOController - Producer del patrón Producer-Consumer.
 * Lee datagramas desde archivo de forma continua (tail-like), 
 * los agrupa en lotes y los encola en DataQueue.
 * 
 * IMPORTANTE: Este controlador hace "tail" del archivo, es decir,
 * espera nuevos datos que el DataReceiver va agregando.
 */
public class CCOController implements Runnable {
    private final String dataFilePath;
    private final DataQueue dataQueue;
    private final int batchSize;
    private volatile boolean running;
    private long totalProcessed;
    private long totalBatches;
    private long lastFilePosition;

    public CCOController(String dataFilePath, DataQueue dataQueue, int batchSize) {
        this.dataFilePath = dataFilePath;
        this.dataQueue = dataQueue;
        this.batchSize = batchSize;
        this.running = false;
        this.totalProcessed = 0;
        this.totalBatches = 0;
        this.lastFilePosition = 0;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[CCOController] Iniciado - monitoreando archivo: " + dataFilePath);
        System.out.println("[CCOController] Tamaño de lote: " + batchSize);
        System.out.println("[CCOController] Modo: tail continuo (espera nuevos datos)");

        List<Datagram> currentBatch = new ArrayList<>();
        int emptyReads = 0;
        boolean headerSkipped = false;

        while (running) {
            try {
                // Usar RandomAccessFile para poder hacer "tail"
                java.io.File file = new java.io.File(dataFilePath);
                
                // Esperar a que el archivo exista
                if (!file.exists()) {
                    if (emptyReads % 20 == 0) {
                        System.out.println("[CCOController] Esperando archivo: " + dataFilePath);
                    }
                    emptyReads++;
                    Thread.sleep(1000);
                    continue;
                }

                long fileLength = file.length();
                
                // Si no hay datos nuevos, esperar
                if (fileLength <= lastFilePosition) {
                    emptyReads++;
                    if (emptyReads % 60 == 0) {
                        System.out.println("[CCOController] Esperando más datos... (posición: " + lastFilePosition + ")");
                    }
                    Thread.sleep(500);
                    continue;
                }

                // Leer nuevos datos desde la última posición
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    raf.seek(lastFilePosition);
                    
                    String line;
                    while (running && (line = raf.readLine()) != null) {
                        // Convertir de ISO-8859-1 (default de readLine) a UTF-8
                        line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        
                        // Saltar header si es la primera línea
                        if (!headerSkipped && lastFilePosition == 0) {
                            headerSkipped = true;
                            if (line.contains("eventType") || line.contains("registerDate") || line.contains("registerdate")) {
                                lastFilePosition = raf.getFilePointer();
                                continue;
                            }
                        }

                        // Saltar líneas vacías
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        try {
                            // Parsear datagrama
                            Datagram datagram = Datagram.fromCsvLine(line);
                            currentBatch.add(datagram);
                            totalProcessed++;
                            emptyReads = 0; // Reiniciar contador

                            // Si el lote está completo, encolarlo
                            if (currentBatch.size() >= batchSize) {
                                enqueueBatch(currentBatch);
                                currentBatch = new ArrayList<>();
                            }

                        } catch (Exception e) {
                            // Log solo cada ciertos errores para no saturar
                            if (totalProcessed % 10000 == 0) {
                                System.err.println("[CCOController] Error parseando (ignorando): " + e.getMessage());
                            }
                        }
                    }
                    
                    // Actualizar posición
                    lastFilePosition = raf.getFilePointer();
                }

            } catch (InterruptedException e) {
                System.out.println("[CCOController] Interrumpido");
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("[CCOController] Error de lectura: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Encolar el último lote si tiene datos
        if (!currentBatch.isEmpty()) {
            try {
                enqueueBatch(currentBatch);
            } catch (InterruptedException e) {
                System.err.println("[CCOController] Interrumpido al encolar último lote");
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n[CCOController] Detenido");
        System.out.println("  - Total datagramas procesados: " + totalProcessed);
        System.out.println("  - Total lotes encolados: " + totalBatches);
        running = false;
    }    /**
     * Encola un lote en DataQueue (bloqueante).
     */
    private void enqueueBatch(List<Datagram> batch) throws InterruptedException {
        dataQueue.enqueueLote(batch);
        totalBatches++;
        
        if (totalBatches % 10 == 0) {
            System.out.printf("[CCOController] Lotes encolados: %d | Datagramas: %d | Cola: %d/%d lotes\n",
                totalBatches, totalProcessed, dataQueue.size(), dataQueue.getCapacity());
        }
    }
    
    /**
     * Detiene el procesamiento.
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Verifica si está en ejecución.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Total de datagramas procesados.
     */
    public long getTotalProcessed() {
        return totalProcessed;
    }
    
    /**
     * Total de lotes encolados.
     */
    public long getTotalBatches() {
        return totalBatches;
    }
}
