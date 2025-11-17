package com.mio.visualization;

import com.mio.graph.TransportGraph;
import com.mio.model.Line;
import com.mio.model.Stop;
import com.mio.model.Arco;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Visualizador 2D del grafo de transporte
 * Genera imágenes JPG del grafo completo o de líneas específicas
 */
public class GraphVisualizer {
    private static final int WIDTH = 3000;
    private static final int HEIGHT = 2000;
    private static final int MARGIN = 100;
    private static final int NODE_RADIUS = 8;
    private static final float ARROW_SIZE = 10f;
    
    private TransportGraph graph;
    private Map<Integer, Point> nodePositions;
    private Random random;
    
    public GraphVisualizer(TransportGraph graph) {
        this.graph = graph;
        this.nodePositions = new HashMap<>();
        this.random = new Random(42); // Seed fijo para posiciones consistentes
    }
    
    /**
     * Genera visualización de una línea específica
     */
    public void visualizarLinea(int lineId, String outputPath) throws IOException {
        Map<Integer, Map<Integer, List<Arco>>> lineData = graph.getArcosPorLinea(lineId);
        if (lineData == null) {
            System.out.println("Línea no encontrada: " + lineId);
            return;
        }
        
        Line line = graph.getLines().get(lineId);
        String title = line != null ? line.getShortName() + " - " + line.getDescription() : "Línea " + lineId;
        
        // Recopilar todas las paradas de esta línea
        Set<Integer> stops = new HashSet<>();
        List<Arco> allArcos = new ArrayList<>();
        
        for (Map<Integer, List<Arco>> orientaciones : lineData.values()) {
            for (List<Arco> arcos : orientaciones.values()) {
                allArcos.addAll(arcos);
                for (Arco arco : arcos) {
                    stops.add(arco.getOrigen().getStopId());
                    stops.add(arco.getDestino().getStopId());
                }
            }
        }
        
        // Calcular posiciones
        calcularPosicionesCirculares(stops);
        
        // Crear imagen
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Configurar renderizado de alta calidad
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fondo blanco
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Título
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString(title, MARGIN, 50);
        
        // Dibujar arcos por variante con colores diferentes
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        
        for (Map.Entry<Integer, Map<Integer, List<Arco>>> orientEntry : lineData.entrySet()) {
            int orientation = orientEntry.getKey();
            String orientLabel = orientation == 0 ? "IDA" : "VUELTA";
            
            for (Map.Entry<Integer, List<Arco>> variantEntry : orientEntry.getValue().entrySet()) {
                int variant = variantEntry.getKey();
                List<Arco> arcos = variantEntry.getValue();
                
                Color color = colors[colorIndex % colors.length];
                colorIndex++;
                
                // Dibujar arcos
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(2));
                
                for (Arco arco : arcos) {
                    Point p1 = nodePositions.get(arco.getOrigen().getStopId());
                    Point p2 = nodePositions.get(arco.getDestino().getStopId());
                    
                    if (p1 != null && p2 != null) {
                        drawArrow(g2d, p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
        }
        
        // Dibujar nodos
        g2d.setStroke(new BasicStroke(1));
        for (int stopId : stops) {
            Point p = nodePositions.get(stopId);
            if (p != null) {
                Stop stop = graph.getStops().get(stopId);
                
                // Círculo del nodo
                g2d.setColor(Color.WHITE);
                g2d.fill(new Ellipse2D.Double(p.x - NODE_RADIUS, p.y - NODE_RADIUS, 
                                              NODE_RADIUS * 2, NODE_RADIUS * 2));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Ellipse2D.Double(p.x - NODE_RADIUS, p.y - NODE_RADIUS, 
                                              NODE_RADIUS * 2, NODE_RADIUS * 2));
                
                // Etiqueta del nodo
                if (stop != null) {
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString(stop.getShortName(), p.x + NODE_RADIUS + 2, p.y + 4);
                }
            }
        }
        
        // Leyenda
        dibujarLeyenda(g2d, lineData, colors);
        
        g2d.dispose();
        
        // Guardar imagen
        File outputFile = new File(outputPath);
        ImageIO.write(image, "jpg", outputFile);
        System.out.println("✓ Imagen generada: " + outputFile.getAbsolutePath());
    }
    
    /**
     * Genera visualización del grafo completo (todas las líneas)
     */
    public void visualizarGrafoCompleto(String outputPath) throws IOException {
        // Recopilar todas las paradas
        Set<Integer> allStops = new HashSet<>(graph.getStops().keySet());
        
        // Calcular posiciones usando layout de fuerza
        calcularPosicionesForceDirected(allStops);
        
        // Crear imagen grande
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Configurar renderizado
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo blanco
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Título
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.drawString("Sistema MIO - Cali, Colombia", MARGIN, 50);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Total: " + graph.getLines().size() + " líneas, " + 
                       graph.getStops().size() + " paradas", MARGIN, 80);
        
        // Dibujar todos los arcos (semitransparentes para ver superposiciones)
        g2d.setColor(new Color(100, 100, 255, 50));
        g2d.setStroke(new BasicStroke(1));
        
        for (Map<Integer, Map<Integer, List<Arco>>> lineData : getAllArcos()) {
            for (Map<Integer, List<Arco>> orientaciones : lineData.values()) {
                for (List<Arco> arcos : orientaciones.values()) {
                    for (Arco arco : arcos) {
                        Point p1 = nodePositions.get(arco.getOrigen().getStopId());
                        Point p2 = nodePositions.get(arco.getDestino().getStopId());
                        
                        if (p1 != null && p2 != null) {
                            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                        }
                    }
                }
            }
        }
        
        // Dibujar nodos
        g2d.setStroke(new BasicStroke(1));
        for (int stopId : allStops) {
            Point p = nodePositions.get(stopId);
            if (p != null) {
                g2d.setColor(Color.RED);
                g2d.fill(new Ellipse2D.Double(p.x - 3, p.y - 3, 6, 6));
            }
        }
        
        g2d.dispose();
        
        // Guardar imagen
        File outputFile = new File(outputPath);
        ImageIO.write(image, "jpg", outputFile);
        System.out.println("✓ Imagen del grafo completo generada: " + outputFile.getAbsolutePath());
    }
    
    /**
     * Calcula posiciones de nodos en círculo
     */
    private void calcularPosicionesCirculares(Set<Integer> stops) {
        List<Integer> stopList = new ArrayList<>(stops);
        int n = stopList.size();
        
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;
        int radius = Math.min(WIDTH, HEIGHT) / 2 - MARGIN;
        
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            nodePositions.put(stopList.get(i), new Point(x, y));
        }
    }
    
    /**
     * Calcula posiciones usando algoritmo force-directed simplificado
     */
    private void calcularPosicionesForceDirected(Set<Integer> stops) {
        // Inicializar posiciones aleatorias
        for (int stopId : stops) {
            int x = MARGIN + random.nextInt(WIDTH - 2 * MARGIN);
            int y = MARGIN + 50 + random.nextInt(HEIGHT - 2 * MARGIN - 50);
            nodePositions.put(stopId, new Point(x, y));
        }
        
        // Simplificación: usar posiciones basadas en coordenadas geográficas si están disponibles
        for (int stopId : stops) {
            Stop stop = graph.getStops().get(stopId);
            if (stop != null && stop.getDecimalLatitude() != 0 && stop.getDecimalLongitude() != 0) {
                // Normalizar coordenadas geográficas a espacio de imagen
                double lat = stop.getDecimalLatitude();
                double lon = stop.getDecimalLongitude();
                
                // Encontrar rangos
                double minLat = 3.35, maxLat = 3.50;
                double minLon = -76.60, maxLon = -76.45;
                
                int x = MARGIN + (int)((lon - minLon) / (maxLon - minLon) * (WIDTH - 2 * MARGIN));
                int y = HEIGHT - MARGIN - (int)((lat - minLat) / (maxLat - minLat) * (HEIGHT - 2 * MARGIN - 50));
                
                nodePositions.put(stopId, new Point(x, y));
            }
        }
    }
    
    /**
     * Dibuja una flecha de p1 a p2
     */
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2);
        
        // Calcular ángulo
        double angle = Math.atan2(y2 - y1, x2 - x1);
        
        // Puntos de la punta de flecha
        int arrowX1 = (int)(x2 - ARROW_SIZE * Math.cos(angle - Math.PI / 6));
        int arrowY1 = (int)(y2 - ARROW_SIZE * Math.sin(angle - Math.PI / 6));
        int arrowX2 = (int)(x2 - ARROW_SIZE * Math.cos(angle + Math.PI / 6));
        int arrowY2 = (int)(y2 - ARROW_SIZE * Math.sin(angle + Math.PI / 6));
        
        g2d.drawLine(x2, y2, arrowX1, arrowY1);
        g2d.drawLine(x2, y2, arrowX2, arrowY2);
    }
    
    /**
     * Dibuja leyenda con información de variantes
     */
    private void dibujarLeyenda(Graphics2D g2d, Map<Integer, Map<Integer, List<Arco>>> lineData, Color[] colors) {
        int x = WIDTH - 350;
        int y = 100;
        
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(x - 10, y - 20, 330, 200);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 10, y - 20, 330, 200);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Variantes:", x, y);
        
        y += 20;
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        int colorIndex = 0;
        for (Map.Entry<Integer, Map<Integer, List<Arco>>> orientEntry : lineData.entrySet()) {
            int orientation = orientEntry.getKey();
            String orientLabel = orientation == 0 ? "IDA" : "VUELTA";
            
            for (Map.Entry<Integer, List<Arco>> variantEntry : orientEntry.getValue().entrySet()) {
                int variant = variantEntry.getKey();
                int arcoCount = variantEntry.getValue().size();
                
                Color color = colors[colorIndex % colors.length];
                g2d.setColor(color);
                g2d.fillRect(x, y - 8, 15, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y - 8, 15, 10);
                
                g2d.drawString(orientLabel + " - Var. " + variant + " (" + arcoCount + " arcos)", x + 20, y);
                
                y += 18;
                colorIndex++;
            }
        }
    }
    
    /**
     * Obtiene todos los arcos del grafo
     */
    private List<Map<Integer, Map<Integer, List<Arco>>>> getAllArcos() {
        List<Map<Integer, Map<Integer, List<Arco>>>> result = new ArrayList<>();
        for (int lineId : graph.getLines().keySet()) {
            Map<Integer, Map<Integer, List<Arco>>> lineData = graph.getArcosPorLinea(lineId);
            if (lineData != null) {
                result.add(lineData);
            }
        }
        return result;
    }
}
