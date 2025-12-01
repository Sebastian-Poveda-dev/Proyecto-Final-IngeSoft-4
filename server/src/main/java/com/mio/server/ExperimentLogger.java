package com.mio.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

/**
 * ExperimentLogger - Registra métricas de experimentos para análisis.
 * Exporta datos a CSV para generar gráficos de rendimiento.
 */
public class ExperimentLogger {
    
    private static final String LOG_FILE = "data/experiment_results.csv";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String experimentId;
    private final int numWorkers;
    private final String datasetSize;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long datagramasRecibidos;
    private long lotesEncolados;
    private long lotesProcesados;
    private long arcosCalculados;
    
    private volatile boolean running;
    private Thread monitorThread;
    
    // Referencias a componentes del servidor
    private DataReceiverImpl dataReceiver;
    private CCOController ccoController;
    private MasterImpl master;
    private Aggregator aggregator;
    
    public ExperimentLogger(String experimentId, int numWorkers, String datasetSize) {
        this.experimentId = experimentId;
        this.numWorkers = numWorkers;
        this.datasetSize = datasetSize;
        this.running = false;
    }
    
    /**
     * Configura las referencias a los componentes para monitoreo.
     */
    public void setComponents(DataReceiverImpl dataReceiver, CCOController ccoController,
                              MasterImpl master, Aggregator aggregator) {
        this.dataReceiver = dataReceiver;
        this.ccoController = ccoController;
        this.master = master;
        this.aggregator = aggregator;
    }
    
    /**
     * Inicia el registro del experimento.
     */
    public void start() {
        startTime = LocalDateTime.now();
        running = true;
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              EXPERIMENTO INICIADO                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  ID: %-55s ║\n", experimentId);
        System.out.printf("║  Workers: %-50d ║\n", numWorkers);
        System.out.printf("║  Dataset: %-50s ║\n", datasetSize);
        System.out.printf("║  Inicio: %-51s ║\n", startTime.format(FORMATTER));
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Iniciar hilo de monitoreo
        monitorThread = new Thread(this::monitorLoop, "ExperimentMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    /**
     * Loop de monitoreo que imprime estadísticas periódicamente.
     */
    private void monitorLoop() {
        long lastDatagramas = 0;
        long lastTime = System.currentTimeMillis();
        
        while (running) {
            try {
                Thread.sleep(10000); // Cada 10 segundos
                
                if (dataReceiver != null && ccoController != null && aggregator != null) {
                    long currentDatagramas = dataReceiver.getReceivedCount();
                    long currentTime = System.currentTimeMillis();
                    
                    // Calcular throughput
                    double elapsed = (currentTime - lastTime) / 1000.0;
                    double throughput = (currentDatagramas - lastDatagramas) / elapsed;
                    
                    System.out.println("\n┌──────────────────────────────────────────────────────────────┐");
                    System.out.println("│              MÉTRICAS DEL EXPERIMENTO                        │");
                    System.out.println("├──────────────────────────────────────────────────────────────┤");
                    System.out.printf("│  Tiempo transcurrido: %-38s │\n", 
                        formatDuration(Duration.between(startTime, LocalDateTime.now())));
                    System.out.printf("│  Datagramas recibidos: %-37s │\n", 
                        String.format("%,d", currentDatagramas));
                    System.out.printf("│  Lotes encolados: %-42s │\n", 
                        String.format("%,d", ccoController.getTotalBatches()));
                    System.out.printf("│  Lotes procesados: %-41s │\n", 
                        String.format("%,d", aggregator.getTotalLotesProcesados()));
                    System.out.printf("│  Arcos calculados: %-41s │\n", 
                        String.format("%,d", aggregator.getArcoCount()));
                    System.out.printf("│  Throughput actual: %-36s │\n", 
                        String.format("%,.0f dgm/s", throughput));
                    System.out.printf("│  Workers activos: %-42s │\n", 
                        String.format("%d", master != null ? master.getWorkerCount() : 0));
                    System.out.println("└──────────────────────────────────────────────────────────────┘");
                    
                    lastDatagramas = currentDatagramas;
                    lastTime = currentTime;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Finaliza el experimento y guarda los resultados.
     */
    public void stop() {
        running = false;
        endTime = LocalDateTime.now();
        
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        
        // Capturar métricas finales
        if (dataReceiver != null) {
            datagramasRecibidos = dataReceiver.getReceivedCount();
        }
        if (ccoController != null) {
            lotesEncolados = ccoController.getTotalBatches();
        }
        if (aggregator != null) {
            lotesProcesados = aggregator.getTotalLotesProcesados();
            arcosCalculados = aggregator.getArcoCount();
        }
        
        // Calcular métricas
        Duration duration = Duration.between(startTime, endTime);
        double segundos = duration.toMillis() / 1000.0;
        double throughput = datagramasRecibidos / segundos;
        
        // Imprimir resumen
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              EXPERIMENTO FINALIZADO                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  ID: %-55s ║\n", experimentId);
        System.out.printf("║  Workers: %-50d ║\n", numWorkers);
        System.out.printf("║  Dataset: %-50s ║\n", datasetSize);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Inicio: %-51s ║\n", startTime.format(FORMATTER));
        System.out.printf("║  Fin: %-54s ║\n", endTime.format(FORMATTER));
        System.out.printf("║  Duración: %-49s ║\n", formatDuration(duration));
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Datagramas recibidos: %-37s ║\n", String.format("%,d", datagramasRecibidos));
        System.out.printf("║  Lotes encolados: %-42s ║\n", String.format("%,d", lotesEncolados));
        System.out.printf("║  Lotes procesados: %-41s ║\n", String.format("%,d", lotesProcesados));
        System.out.printf("║  Arcos calculados: %-41s ║\n", String.format("%,d", arcosCalculados));
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  THROUGHPUT: %-43s ║\n", String.format("%,.2f datagramas/segundo", throughput));
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Guardar a CSV
        saveToCSV(segundos, throughput);
    }
    
    /**
     * Guarda los resultados del experimento en CSV.
     */
    private void saveToCSV(double segundos, double throughput) {
        boolean fileExists = new java.io.File(LOG_FILE).exists();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            // Escribir header si es archivo nuevo
            if (!fileExists) {
                writer.println("experiment_id,num_workers,dataset_size,start_time,end_time," +
                    "duration_seconds,datagramas_recibidos,lotes_encolados,lotes_procesados," +
                    "arcos_calculados,throughput_dgm_s");
            }
            
            // Escribir datos del experimento
            writer.printf("%s,%d,%s,%s,%s,%.2f,%d,%d,%d,%d,%.2f\n",
                experimentId,
                numWorkers,
                datasetSize,
                startTime.format(FORMATTER),
                endTime.format(FORMATTER),
                segundos,
                datagramasRecibidos,
                lotesEncolados,
                lotesProcesados,
                arcosCalculados,
                throughput
            );
            
            System.out.println("[ExperimentLogger] Resultados guardados en: " + LOG_FILE);
            
        } catch (IOException e) {
            System.err.println("[ExperimentLogger] Error guardando CSV: " + e.getMessage());
        }
    }
    
    /**
     * Formatea una duración a formato legible.
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%.1fs", duration.toMillis() / 1000.0);
        }
    }
    
    // Getters
    public String getExperimentId() { return experimentId; }
    public int getNumWorkers() { return numWorkers; }
    public String getDatasetSize() { return datasetSize; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
