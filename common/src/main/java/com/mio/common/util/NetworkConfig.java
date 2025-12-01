package com.mio.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utilidad para cargar configuración de red desde network.properties
 * 
 * IMPORTANTE: Este archivo lee la configuración de IPs para el sistema distribuido.
 * Modifica config/network.properties para cambiar las direcciones IP.
 */
public class NetworkConfig {
    
    private static final String CONFIG_FILE = "config/network.properties";
    private static Properties properties;
    
    static {
        properties = new Properties();
        loadProperties();
    }
    
    private static void loadProperties() {
        // Primero intentar cargar desde archivo externo
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                properties.load(is);
                System.out.println("[NetworkConfig] Configuración cargada desde: " + configPath.toAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("[NetworkConfig] Error leyendo " + CONFIG_FILE + ": " + e.getMessage());
            }
        }
        
        // Si no existe, usar valores por defecto
        System.out.println("[NetworkConfig] Usando configuración por defecto (localhost)");
        setDefaults();
    }
    
    private static void setDefaults() {
        properties.setProperty("SERVER_HOST", "localhost");
        properties.setProperty("DATA_RECEIVER_PORT", "10000");
        properties.setProperty("MASTER_SERVICE_PORT", "10001");
        properties.setProperty("BUS_TARGET_SERVER", "localhost");
        properties.setProperty("BUS_TARGET_PORT", "10000");
        properties.setProperty("WORKER_MASTER_HOST", "localhost");
        properties.setProperty("WORKER_MASTER_PORT", "10001");
        properties.setProperty("CLIENT_SERVER_HOST", "localhost");
        properties.setProperty("CLIENT_SERVER_PORT", "10002");
    }
    
    public static void reload() {
        loadProperties();
    }
    
    // ===== SERVIDOR =====
    
    public static String getServerHost() {
        return properties.getProperty("SERVER_HOST", "localhost");
    }
    
    public static int getDataReceiverPort() {
        return Integer.parseInt(properties.getProperty("DATA_RECEIVER_PORT", "10000"));
    }
    
    public static int getMasterServicePort() {
        return Integer.parseInt(properties.getProperty("MASTER_SERVICE_PORT", "10001"));
    }
    
    // ===== BUS =====
    
    public static String getBusTargetServer() {
        return properties.getProperty("BUS_TARGET_SERVER", "localhost");
    }
    
    public static int getBusTargetPort() {
        return Integer.parseInt(properties.getProperty("BUS_TARGET_PORT", "10000"));
    }
    
    /**
     * Obtiene el endpoint ICE para que el Bus conecte al DataReceiver
     * Ejemplo: "DataReceiver:default -h 192.168.1.100 -p 10000"
     */
    public static String getBusDataReceiverEndpoint() {
        return String.format("DataReceiver:default -h %s -p %d", 
            getBusTargetServer(), getBusTargetPort());
    }
    
    // ===== WORKER =====
    
    public static String getWorkerMasterHost() {
        return properties.getProperty("WORKER_MASTER_HOST", "localhost");
    }
    
    public static int getWorkerMasterPort() {
        return Integer.parseInt(properties.getProperty("WORKER_MASTER_PORT", "10001"));
    }
    
    /**
     * Obtiene el endpoint ICE para que el Worker conecte al MasterService
     * Ejemplo: "MasterService:default -h 192.168.1.100 -p 10001"
     */
    public static String getWorkerMasterEndpoint() {
        return String.format("MasterService:default -h %s -p %d", 
            getWorkerMasterHost(), getWorkerMasterPort());
    }
    
    // ===== CLIENTE =====
    
    public static String getClientServerHost() {
        return properties.getProperty("CLIENT_SERVER_HOST", "localhost");
    }
    
    public static int getClientServerPort() {
        return Integer.parseInt(properties.getProperty("CLIENT_SERVER_PORT", "10002"));
    }
    
    /**
     * Obtiene el endpoint ICE para que el Cliente conecte al servidor de consultas
     */
    public static String getClientQueryServiceEndpoint() {
        return String.format("GraphQueryService:default -h %s -p %d", 
            getClientServerHost(), getClientServerPort());
    }
    
    // ===== ENDPOINTS SERVIDOR (para crear adapters) =====
    
    /**
     * Endpoint donde el servidor escucha para DataReceiver
     */
    public static String getServerDataReceiverEndpoint() {
        return String.format("default -h %s -p %d", 
            getServerHost(), getDataReceiverPort());
    }
    
    /**
     * Endpoint donde el servidor escucha para MasterService
     */
    public static String getServerMasterEndpoint() {
        return String.format("default -h %s -p %d", 
            getServerHost(), getMasterServicePort());
    }
    
    /**
     * Imprime la configuración actual (útil para debugging)
     */
    public static void printConfig() {
        System.out.println("=".repeat(60));
        System.out.println(" CONFIGURACIÓN DE RED ACTUAL");
        System.out.println("=".repeat(60));
        System.out.println(" Servidor:");
        System.out.println("   HOST: " + getServerHost());
        System.out.println("   DataReceiver Port: " + getDataReceiverPort());
        System.out.println("   MasterService Port: " + getMasterServicePort());
        System.out.println();
        System.out.println(" Bus -> Servidor:");
        System.out.println("   Target: " + getBusTargetServer() + ":" + getBusTargetPort());
        System.out.println("   Endpoint: " + getBusDataReceiverEndpoint());
        System.out.println();
        System.out.println(" Worker -> Master:");
        System.out.println("   Target: " + getWorkerMasterHost() + ":" + getWorkerMasterPort());
        System.out.println("   Endpoint: " + getWorkerMasterEndpoint());
        System.out.println();
        System.out.println(" Cliente -> Servidor:");
        System.out.println("   Target: " + getClientServerHost() + ":" + getClientServerPort());
        System.out.println("=".repeat(60));
    }
}
