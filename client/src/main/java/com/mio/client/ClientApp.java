package com.mio.client;

import MIO.*;
import com.mio.common.util.NetworkConfig;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.util.Scanner;

/**
 * ClientApp - Aplicación cliente para consultas al grafo del sistema MIO.
 * 
 * Permite consultar velocidades promedio por arco, línea y estadísticas generales.
 * 
 * CONFIGURACIÓN DE RED: Edita config/network.properties para cambiar la IP del servidor
 */
public class ClientApp {
    
    private GraphQueryServicePrx queryService;
    private Scanner scanner;
    
    public static void main(String[] args) {
        ClientApp client = new ClientApp();
        client.run(args);
    }
    
    public void run(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           CLIENTE MIO - Consultas al Sistema                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Mostrar configuración de red
        NetworkConfig.printConfig();
        System.out.println();
        
        Communicator communicator = null;
        scanner = new Scanner(System.in);
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Obtener endpoint del servidor desde NetworkConfig
            String queryEndpoint = NetworkConfig.getClientQueryServiceEndpoint();
            System.out.println("Conectando al servidor: " + queryEndpoint);
            
            // Crear proxy al GraphQueryService
            ObjectPrx base = communicator.stringToProxy(queryEndpoint);
            queryService = GraphQueryServicePrx.checkedCast(base);
            
            if (queryService == null) {
                throw new RuntimeException("Proxy inválido al GraphQueryService: " + queryEndpoint);
            }
            
            // Verificar conectividad
            if (queryService.ping()) {
                System.out.println("✓ Conexión establecida con el servidor\n");
            }
            
            // Bucle principal del menú
            boolean continuar = true;
            while (continuar) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();

                switch (opcion) {
                    case "1":
                        consultarVelocidadesPorLinea();
                        break;
                    case "2":
                        consultarTodasLasLineas();
                        break;
                    case "3":
                        consultarArcosMasLentos();
                        break;
                    case "4":
                        consultarArcosMasRapidos();
                        break;
                    case "5":
                        mostrarEstadisticasGenerales();
                        break;
                    case "0":
                    case "q":
                    case "salir":
                        continuar = false;
                        break;
                    default:
                        System.out.println("Opción no válida. Intente de nuevo.\n");
                }
            }            System.out.println("\n¡Hasta luego!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
    
    private void mostrarMenu() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      MENÚ PRINCIPAL                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  1. Ver velocidades de todos los arcos de una línea          ║");
        System.out.println("║  2. Ver lista de todas las líneas                            ║");
        System.out.println("║  3. Ver arcos más lentos (posible congestión)                ║");
        System.out.println("║  4. Ver arcos más rápidos                                    ║");
        System.out.println("║  5. Ver estadísticas generales del sistema                   ║");
        System.out.println("║  0. Salir                                                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.print("\nSeleccione una opción: ");
    }
    
    private void consultarVelocidadArco() {
        System.out.println("\n--- Consultar Velocidad de Arco ---");
        System.out.println("Formato del ID de arco: lineId-stopOrigen-stopDestino");
        System.out.print("Ingrese el ID del arco (ej: 241-1234-5678): ");
        
        String arcoId = scanner.nextLine().trim();
        
        if (arcoId.isEmpty()) {
            System.out.println("ID de arco no puede estar vacío.\n");
            return;
        }
        
        try {
            ArcoInfo info = queryService.getArcoInfo(arcoId);
            
            if (info.observaciones > 0) {
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.println("║              INFORMACIÓN DEL ARCO                            ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.printf("║  ID Arco:            %-39s ║\n", info.arcoId);
                System.out.printf("║  Línea:              %-39d ║\n", info.lineId);
                System.out.printf("║  Parada Origen:      %-39d ║\n", info.stopOrigen);
                System.out.printf("║  Parada Destino:     %-39d ║\n", info.stopDestino);
                System.out.printf("║  Velocidad Promedio: %-35.2f km/h ║\n", info.velocidadPromedio);
                System.out.printf("║  Observaciones:      %-39d ║\n", info.observaciones);
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            } else {
                System.out.println("\n⚠ No se encontró información para el arco: " + arcoId);
                System.out.println("  Verifique que el ID tenga el formato correcto.");
            }
        } catch (Exception e) {
            System.err.println("Error consultando arco: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void consultarVelocidadesPorLinea() {
        System.out.println("\n--- Velocidades por Línea ---");
        System.out.print("Ingrese el ID de la línea: ");
        
        String input = scanner.nextLine().trim();
        
        try {
            int lineId = Integer.parseInt(input);
            ArcoInfo[] arcos = queryService.getVelocidadesPorLinea(lineId);
            
            if (arcos.length == 0) {
                System.out.println("\n⚠ No se encontraron arcos para la línea " + lineId);
            } else {
                // Calcular velocidad promedio
                double velPromedio = queryService.getVelocidadPromedioLinea(lineId);
                
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.printf("║              LÍNEA %d - %d arcos                         \n", lineId, arcos.length);
                System.out.printf("║              Velocidad promedio: %.2f km/h                \n", velPromedio);
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.println("║  ARCO                      │ VELOCIDAD │ OBSERVACIONES       ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                
                for (ArcoInfo arco : arcos) {
                    System.out.printf("║  %-24s │ %7.2f   │ %10d          ║\n",
                        arco.arcoId, arco.velocidadPromedio, arco.observaciones);
                }
                
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: El ID de línea debe ser un número.");
        } catch (Exception e) {
            System.err.println("Error consultando línea: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void consultarTodasLasLineas() {
        System.out.println("\n--- Lista de Todas las Líneas ---");
        
        try {
            LineaInfo[] lineas = queryService.getTodasLasLineas();
            
            if (lineas.length == 0) {
                System.out.println("\n⚠ No hay líneas registradas en el sistema.");
            } else {
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.printf("║              LÍNEAS DEL SISTEMA (%d líneas)                 \n", lineas.length);
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.println("║  LÍNEA │ NOMBRE          │ ARCOS │ VEL. PROMEDIO            ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                
                for (LineaInfo linea : lineas) {
                    System.out.printf("║  %5d │ %-15s │ %5d │ %8.2f km/h            ║\n",
                        linea.lineId, linea.nombre, linea.cantidadArcos, linea.velocidadPromedio);
                }
                
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            }
        } catch (Exception e) {
            System.err.println("Error consultando líneas: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void consultarArcosMasLentos() {
        System.out.println("\n--- Arcos Más Lentos (Posible Congestión) ---");
        System.out.print("¿Cuántos arcos desea ver? (default: 10): ");
        
        String input = scanner.nextLine().trim();
        int limite = 10;
        
        if (!input.isEmpty()) {
            try {
                limite = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Usando valor default: 10");
            }
        }
        
        try {
            ArcoInfo[] arcos = queryService.getArcosMasLentos(limite);
            
            if (arcos.length == 0) {
                System.out.println("\n⚠ No hay arcos con datos de velocidad.");
            } else {
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.printf("║              TOP %d ARCOS MÁS LENTOS                       \n", arcos.length);
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.println("║  #  │ ARCO                      │ VELOCIDAD │ OBS           ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                
                int i = 1;
                for (ArcoInfo arco : arcos) {
                    System.out.printf("║  %2d │ %-24s │ %7.2f   │ %6d        ║\n",
                        i++, arco.arcoId, arco.velocidadPromedio, arco.observaciones);
                }
                
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            }
        } catch (Exception e) {
            System.err.println("Error consultando arcos lentos: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void consultarArcosMasRapidos() {
        System.out.println("\n--- Arcos Más Rápidos ---");
        System.out.print("¿Cuántos arcos desea ver? (default: 10): ");
        
        String input = scanner.nextLine().trim();
        int limite = 10;
        
        if (!input.isEmpty()) {
            try {
                limite = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Usando valor default: 10");
            }
        }
        
        try {
            ArcoInfo[] arcos = queryService.getArcosMasRapidos(limite);
            
            if (arcos.length == 0) {
                System.out.println("\n⚠ No hay arcos con datos de velocidad.");
            } else {
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.printf("║              TOP %d ARCOS MÁS RÁPIDOS                      \n", arcos.length);
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.println("║  #  │ ARCO                      │ VELOCIDAD │ OBS           ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                
                int i = 1;
                for (ArcoInfo arco : arcos) {
                    System.out.printf("║  %2d │ %-24s │ %7.2f   │ %6d        ║\n",
                        i++, arco.arcoId, arco.velocidadPromedio, arco.observaciones);
                }
                
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            }
        } catch (Exception e) {
            System.err.println("Error consultando arcos rápidos: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void mostrarEstadisticasGenerales() {
        System.out.println("\n--- Estadísticas Generales del Sistema ---");
        
        try {
            String stats = queryService.getEstadisticasGenerales();
            System.out.println();
            System.out.println(stats);
        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas: " + e.getMessage());
        }
        System.out.println();
    }
}
