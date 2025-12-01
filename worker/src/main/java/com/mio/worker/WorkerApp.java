package com.mio.worker;

import MIO.*;
import com.mio.common.util.NetworkConfig;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.util.UUID;

/**
 * WorkerApp - Aplicación principal del Worker.
 * 
 * Se conecta al MasterService vía ICE, solicita lotes de datagramas,
 * los procesa con un ThreadPool y envía resultados parciales al Master.
 * 
 * CONFIGURACIÓN DE RED: Edita config/network.properties para cambiar la IP del Master
 */
public class WorkerApp {

    private static final int DEFAULT_THREAD_POOL_SIZE = 30;    public static void main(String[] args) {
        // Generar ID único para este worker
        String workerId = "Worker-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Configuración
        int threadPoolSize = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_THREAD_POOL_SIZE;
        
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              WORKER MIO - Procesador Distribuido             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Worker ID: %-48s ║\n", workerId);
        System.out.printf("║  ThreadPool Size: %-42d ║\n", threadPoolSize);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Mostrar configuración de red
        NetworkConfig.printConfig();
        System.out.println();
        
        Communicator communicator = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Obtener endpoint del Master desde NetworkConfig
            String masterEndpoint = NetworkConfig.getWorkerMasterEndpoint();
            System.out.println("Conectando al Master: " + masterEndpoint);
            
            // Crear proxy al MasterService
            ObjectPrx base = communicator.stringToProxy(masterEndpoint);
            MasterServicePrx master = MasterServicePrx.checkedCast(base);
            
            if (master == null) {
                throw new RuntimeException("Proxy inválido al MasterService: " + masterEndpoint);
            }
            
            System.out.println("✓ Conexión establecida con el Master");
            
            // Registrar este worker en el Master
            boolean registrado = master.registrarWorker(workerId);
            if (registrado) {
                System.out.println("✓ Worker registrado exitosamente");
            } else {
                System.out.println("⚠ Worker ya estaba registrado");
            }
            
            // Crear procesador de lotes con ThreadPool
            LoteProcessor processor = new LoteProcessor(threadPoolSize);
            
            System.out.println("\n--- Iniciando bucle de procesamiento ---\n");
            
            // Estadísticas
            long lotesRecibidos = 0;
            long lotesVacios = 0;
            long totalDatagramas = 0;
            long startTime = System.currentTimeMillis();
            
            // Bucle principal: solicitar lotes mientras haya
            while (true) {
                // Solicitar lote al Master
                LoteDatagram lote = master.getLote();
                
                // Verificar si el lote es válido
                if (lote.loteId < 0 || lote.datagramas == null || lote.datagramas.length == 0) {
                    lotesVacios++;

                    // Esperar antes de reintentar
                    if (lotesVacios % 20 == 0) {
                        System.out.println("[Worker] Cola vacía, esperando más lotes... (intentos vacíos: " + lotesVacios + ")");
                    }
                    
                    // Solo terminar después de muchos intentos vacíos consecutivos (5 minutos aprox)
                    if (lotesVacios > 300) {
                        System.out.println("\n[Worker] Sin lotes por mucho tiempo. Finalizando...");
                        break;
                    }
                    
                    Thread.sleep(1000); // Esperar 1 segundo antes de reintentar
                    continue;
                }
                
                // Reiniciar contador de lotes vacíos cuando recibimos uno válido
                lotesVacios = 0;                lotesRecibidos++;
                totalDatagramas += lote.datagramas.length;
                
                System.out.printf("[Worker] Lote #%d recibido - %d datagramas\n", 
                    lote.loteId, lote.datagramas.length);
                
                // Procesar el lote con ThreadPool
                ResultadosParciales resultados = processor.procesarLote(lote, workerId);
                
                // Enviar resultados parciales al Master
                master.sendResultadosParciales(resultados);
                
                System.out.printf("[Worker] Lote #%d procesado - %d arcos calculados\n",
                    lote.loteId, resultados.resultados != null ? resultados.resultados.length : 0);
                
                // Mostrar estadísticas cada 5 lotes
                if (lotesRecibidos % 5 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = totalDatagramas / (elapsed / 1000.0);
                    System.out.printf("[Worker] Stats: %d lotes | %d datagramas | %.0f dgm/s\n",
                        lotesRecibidos, totalDatagramas, rate);
                }
            }
            
            // Detener el procesador
            processor.shutdown();
            
            // Mostrar resumen final
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    RESUMEN FINAL                             ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.printf("║  Lotes procesados:     %-37d ║\n", lotesRecibidos);
            System.out.printf("║  Datagramas totales:   %-37d ║\n", totalDatagramas);
            System.out.printf("║  Tiempo total:         %-33.2f s ║\n", totalTime / 1000.0);
            System.out.printf("║  Velocidad promedio:   %-33.0f dgm/s ║\n", 
                totalDatagramas / (totalTime / 1000.0));
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            
        } catch (InterruptedException e) {
            System.err.println("[Worker] Interrumpido: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[Worker] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
}
