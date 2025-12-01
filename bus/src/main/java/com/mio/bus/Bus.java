package com.mio.bus;

import com.mio.model.Datagram;
import com.mio.common.util.NetworkConfig;
import MIO.DataReceiverPrx;
import MIO.DatagramStruct;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Clase Bus que simula un bus enviando datagramas al servidor.
 * Lee el archivo datagrams4streaming.csv y envía cada datagrama vía ICE.
 */
public class Bus {
    private static final String DEFAULT_CSV_PATH = "data/datagrams4streaming.csv";
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String csvPath;
    private final String serverEndpoint;
    private final int delayMs;
    
    public Bus(String csvPath, String serverEndpoint, int delayMs) {
        this.csvPath = csvPath;
        this.serverEndpoint = serverEndpoint;
        this.delayMs = delayMs;
    }
    
    /**
     * Inicia el envío de datagramas al servidor.
     */
    public void start() {
        Communicator communicator = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize();
            
            // Crear proxy al servidor
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(serverEndpoint);
            DataReceiverPrx receiver = DataReceiverPrx.checkedCast(base);
            
            if (receiver == null) {
                throw new RuntimeException("Proxy inválido: " + serverEndpoint);
            }
            
            // Verificar conectividad
            System.out.println("Conectando al servidor...");
            if (receiver.ping()) {
                System.out.println("✓ Conexión establecida con el servidor");
            }
            
            // Leer y enviar datagramas
            int enviados = 0;
            int errores = 0;
            
            System.out.println("\nIniciando envío de datagramas desde: " + csvPath);
            System.out.println("Delay entre envíos: " + delayMs + " ms\n");
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(csvPath), 
                        StandardCharsets.UTF_8))) {
                
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    // Saltar header si existe
                    if (isFirstLine) {
                        isFirstLine = false;
                        // Si la primera línea tiene "eventType" o "registerdate", es header
                        if (line.contains("eventType") || line.contains("registerdate")) {
                            continue;
                        }
                    }
                    
                    try {
                        // Parsear línea a Datagram
                        Datagram datagram = Datagram.fromCsvLine(line);
                        
                        // Convertir a estructura ICE
                        DatagramStruct iceStruct = toIceStruct(datagram);
                        
                        // Enviar al servidor
                        boolean success = receiver.sendDatagram(iceStruct);
                        
                        if (success) {
                            enviados++;
                            if (enviados % 100 == 0) {
                                System.out.printf("Enviados: %d datagramas...\n", enviados);
                            }
                        } else {
                            errores++;
                            System.err.println("El servidor rechazó el datagrama");
                        }
                        
                        // Delay para simular streaming
                        if (delayMs > 0) {
                            Thread.sleep(delayMs);
                        }
                        
                    } catch (Exception e) {
                        errores++;
                        System.err.println("Error procesando línea: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("\n=== Resumen ===");
            System.out.println("Datagramas enviados: " + enviados);
            System.out.println("Errores: " + errores);
            
        } catch (Exception e) {
            System.err.println("Error en Bus: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                try {
                    communicator.destroy();
                } catch (Exception e) {
                    System.err.println("Error cerrando comunicator: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Convierte un Datagram Java a DatagramStruct de ICE.
     */
    private DatagramStruct toIceStruct(Datagram d) {
        DatagramStruct ice = new DatagramStruct();
        ice.eventType = d.getEventType();
        ice.registerDate = d.getRegisterDate() != null ? 
            d.getRegisterDate().format(DATE_FORMATTER) : "";
        ice.stopId = d.getStopId();
        ice.odometer = d.getOdometer();
        ice.latitude = d.getLatitude();
        ice.longitude = d.getLongitude();
        ice.taskId = d.getTaskId();
        ice.lineId = d.getLineId();
        ice.tripId = d.getTripId();
        ice.unknown1 = d.getUnknown1();
        ice.datagramDate = d.getDatagramDate() != null ? 
            d.getDatagramDate().format(DATE_FORMATTER) : "";
        ice.busId = d.getBusId();
        return ice;
    }
    
    /**
     * Punto de entrada principal.
     * Argumentos: [csvPath] [serverEndpoint] [delayMs]
     * 
     * CONFIGURACIÓN DE RED: Edita config/network.properties para cambiar la IP del servidor
     */
    public static void main(String[] args) {
        // Mostrar configuración de red
        System.out.println("=== Bus MIO - Emisor de Datagramas ===\n");
        NetworkConfig.printConfig();
        System.out.println();

        String csvPath = args.length > 0 ? args[0] : DEFAULT_CSV_PATH;
        // Usar NetworkConfig para obtener endpoint del servidor
        String serverEndpoint = args.length > 1 ? args[1] : NetworkConfig.getBusDataReceiverEndpoint();
        int delayMs = args.length > 2 ? Integer.parseInt(args[2]) : 0; // Default 0ms (sin delay)        System.out.println("=== Bus MIO - Emisor de Datagramas ===");
        System.out.println("CSV: " + csvPath);
        System.out.println("Servidor: " + serverEndpoint);
        System.out.println("Delay: " + delayMs + " ms\n");
        
        Bus bus = new Bus(csvPath, serverEndpoint, delayMs);
        bus.start();
    }
}
