package com.mio.server;

import com.mio.common.util.NetworkConfig;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

/**
 * Aplicación servidor que integra todos los componentes:
 * - DataReceiver: recibe datagramas de buses (puerto 10000)
 * - CCOController: lee archivo y encola lotes (Producer)
 * - Master: despacha lotes a Workers y recibe resultados (puerto 10001)
 * - Aggregator: combina resultados parciales de Workers
 * - ExperimentLogger: registra métricas para análisis de rendimiento
 *
 * CONFIGURACIÓN DE RED: Edita config/network.properties para cambiar IPs
 * 
 * ARGUMENTOS PARA EXPERIMENTOS:
 *   java -jar server.jar [dataFile] [batchSize] [numWorkers] [datasetSize] [experimentId]
 *   Ejemplo: java -jar server.jar data/received_datagrams.csv 10000 4 "1M" "exp_001"
 */
public class ServerApp {
    private static final String DEFAULT_DATA_FILE = "data/received_datagrams.csv";
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int DEFAULT_QUEUE_CAPACITY = 100;

    public static void main(String[] args) {
        String dataFile = args.length > 0 ? args[0] : DEFAULT_DATA_FILE;
        int batchSize = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_BATCH_SIZE;
        
        // Parámetros para experimentos
        int numWorkers = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        String datasetSize = args.length > 3 ? args[3] : "unknown";
        String experimentId = args.length > 4 ? args[4] : "exp_" + System.currentTimeMillis();
        
        // Crear logger de experimentos si se especificaron parámetros
        ExperimentLogger experimentLogger = null;
        if (args.length > 2) {
            experimentLogger = new ExperimentLogger(experimentId, numWorkers, datasetSize);
        }        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         SERVIDOR MIO - Sistema Distribuido                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  DataReceiver + CCOController + Master + Aggregator          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Mostrar configuración de red
        NetworkConfig.printConfig();
        System.out.println();
        
        Communicator communicator = null;
        Thread ccoThread = null;
        MasterImpl master = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // ============================================================
            // ADAPTER 1: DataReceiver (recibe datagramas de Buses)
            // Puerto: DATA_RECEIVER_PORT (default 10000)
            // ============================================================
            String dataReceiverEndpoint = NetworkConfig.getServerDataReceiverEndpoint();
            ObjectAdapter dataReceiverAdapter = communicator.createObjectAdapterWithEndpoints(
                "DataReceiverAdapter", 
                dataReceiverEndpoint
            );
            
            // Crear repositorio de datagramas
            DatagramRepository repository = new DatagramRepository(dataFile);
            System.out.println("✓ Repositorio de datagramas: " + dataFile);
            
            // Crear e instalar DataReceiver
            DataReceiverImpl receiver = new DataReceiverImpl(repository);
            dataReceiverAdapter.add(receiver, Util.stringToIdentity("DataReceiver"));
            dataReceiverAdapter.activate();
            System.out.println("✓ DataReceiver activo en: " + dataReceiverEndpoint);
            
            // ============================================================
            // DATAQUEUE + CCOCONTROLLER (Producer)
            // ============================================================
            DataQueue dataQueue = new DataQueue(DEFAULT_QUEUE_CAPACITY);
            System.out.println("✓ DataQueue creada (capacidad: " + DEFAULT_QUEUE_CAPACITY + " lotes)");
            
            CCOController ccoController = new CCOController(dataFile, dataQueue, batchSize);
            ccoThread = new Thread(ccoController, "CCOController-Thread");
            ccoThread.start();
            System.out.println("✓ CCOController iniciado (batch: " + batchSize + ")");
            
            // ============================================================
            // ADAPTER 2: MasterService (Workers solicitan lotes aquí)
            // Puerto: MASTER_SERVICE_PORT (default 10001)
            // ============================================================
            String masterEndpoint = NetworkConfig.getServerMasterEndpoint();
            ObjectAdapter masterAdapter = communicator.createObjectAdapterWithEndpoints(
                "MasterAdapter", 
                masterEndpoint
            );
            
            // Crear Aggregator para combinar resultados
            Aggregator aggregator = new Aggregator();
            System.out.println("✓ Aggregator creado");

            // Crear Master (Consumer de DataQueue, despacha a Workers)
            master = new MasterImpl(dataQueue, aggregator);
            masterAdapter.add(master, Util.stringToIdentity("MasterService"));
            masterAdapter.activate();
            System.out.println("✓ MasterService activo en: " + masterEndpoint);

            // Configurar y arrancar ExperimentLogger si está habilitado
            final ExperimentLogger expLogger = experimentLogger;
            if (expLogger != null) {
                expLogger.setComponents(receiver, ccoController, master, aggregator);
                expLogger.start();
            }            // ============================================================
            // ADAPTER 3: GraphQueryService (Clientes consultan aquí)
            // Puerto: CLIENT_SERVER_PORT (default 10002)
            // ============================================================
            String queryEndpoint = String.format("default -h %s -p %d",
                NetworkConfig.getServerHost(), NetworkConfig.getClientServerPort());
            ObjectAdapter queryAdapter = communicator.createObjectAdapterWithEndpoints(
                "QueryAdapter",
                queryEndpoint
            );
            
            // Crear servicio de consultas usando el Aggregator
            GraphQueryServiceImpl queryService = new GraphQueryServiceImpl(aggregator);
            queryAdapter.add(queryService, Util.stringToIdentity("GraphQueryService"));
            queryAdapter.activate();
            System.out.println("✓ GraphQueryService activo en: " + queryEndpoint);
            
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    SISTEMA ACTIVO                            ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║  DataReceiver     → Esperando datagramas de buses            ║");
            System.out.println("║  CCOController    → Leyendo archivo y encolando lotes        ║");
            System.out.println("║  MasterService    → Esperando conexiones de Workers          ║");
            System.out.println("║  Aggregator       → Listo para combinar resultados           ║");
            System.out.println("║  GraphQueryService→ Esperando consultas de clientes          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║  Presiona Ctrl+C para detener                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            
            // Capturar referencia final para usar en lambda
            final MasterImpl masterRef = master;
            final CCOController ccoRef = ccoController;
            
            // Hilo para mostrar estadísticas periódicamente
            Thread statsThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(15000); // Cada 15 segundos
                        System.out.println();
                        System.out.println("╔══════════════════════════════════════════════════════════════╗");
                        System.out.println("║                    ESTADÍSTICAS                              ║");
                        System.out.println("╠══════════════════════════════════════════════════════════════╣");
                        System.out.printf("║  DataReceiver  → Recibidos: %-6d | Rechazados: %-6d      ║\n",
                            receiver.getReceivedCount(),
                            receiver.getRejectedCount());
                        System.out.printf("║  Archivo       → Total registros: %-6d                     ║\n",
                            repository.countInFile());
                        System.out.printf("║  CCOController → Procesados: %-6d | Lotes: %-6d          ║\n",
                            ccoRef.getTotalProcessed(),
                            ccoRef.getTotalBatches());
                        System.out.printf("║  DataQueue     → Lotes pendientes: %-6d                    ║\n",
                            dataQueue.size());
                        System.out.printf("║  Master        → Workers: %-3d | Lotes enviados: %-6d      ║\n",
                            masterRef.getWorkerCount(),
                            masterRef.getLotesDespachados());
                        System.out.printf("║  Aggregator    → Arcos procesados: %-6d                    ║\n",
                            aggregator.getArcoCount());
                        System.out.println("╚══════════════════════════════════════════════════════════════╝");
                    }
                } catch (InterruptedException e) {
                    // Thread interrumpido, salir
                }
            });
            statsThread.setDaemon(true);
            statsThread.start();
            
            // Mantener servidor activo
            communicator.waitForShutdown();

            // Detener componentes
            ccoController.stop();
            
            // Finalizar experimento y guardar métricas
            if (expLogger != null) {
                expLogger.stop();
            }

        } catch (Exception e) {
            System.err.println("Error en servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                try {
                    communicator.destroy();
                    System.out.println("\nServidor detenido.");
                } catch (Exception e) {
                    System.err.println("Error cerrando communicator: " + e.getMessage());
                }
            }
            
            // Esperar a que CCOController termine
            if (ccoThread != null) {
                try {
                    ccoThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
