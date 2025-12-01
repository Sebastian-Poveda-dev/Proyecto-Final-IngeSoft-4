package com.mio.server;

import MIO.ArcoResult;
import MIO.ResultadosParciales;
import com.mio.model.Arco;
import com.mio.graph.TransportGraph;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator - Combina resultados parciales de los Workers.
 * Mantiene estructuras para calcular velocidades promedio por arco.
 */
public class Aggregator {
    // Mapas thread-safe para acumular resultados
    private final Map<String, Double> sumaVelocidadesPorArco;
    private final Map<String, Integer> conteoPorArco;
    
    // Referencia al grafo para actualizar pesos
    private TransportGraph graph;
    
    // Estadísticas
    private long totalLotesProcesados;
    private long totalResultadosRecibidos;
    
    public Aggregator() {
        this.sumaVelocidadesPorArco = new ConcurrentHashMap<>();
        this.conteoPorArco = new ConcurrentHashMap<>();
        this.totalLotesProcesados = 0;
        this.totalResultadosRecibidos = 0;
    }
    
    /**
     * Establece el grafo para actualizar pesos.
     */
    public void setGraph(TransportGraph graph) {
        this.graph = graph;
    }
    
    /**
     * Agrega resultados parciales de un Worker.
     */
    public synchronized void agregarResultadosParciales(ResultadosParciales resultados) {
        if (resultados.resultados == null) return;
        
        for (ArcoResult arcoResult : resultados.resultados) {
            String arcoId = arcoResult.arcoId;
            
            // Acumular suma de velocidades
            sumaVelocidadesPorArco.merge(arcoId, arcoResult.sumaVelocidades, Double::sum);
            
            // Acumular conteo
            conteoPorArco.merge(arcoId, arcoResult.conteo, Integer::sum);
            
            totalResultadosRecibidos++;
        }
        
        totalLotesProcesados++;
        
        if (totalLotesProcesados % 10 == 0) {
            System.out.printf("[Aggregator] Lotes procesados: %d | Arcos únicos: %d | Resultados totales: %d\n",
                totalLotesProcesados, sumaVelocidadesPorArco.size(), totalResultadosRecibidos);
        }
    }
    
    /**
     * Calcula la velocidad promedio de un arco.
     */
    public double getVelocidadPromedio(String arcoId) {
        Double suma = sumaVelocidadesPorArco.get(arcoId);
        Integer conteo = conteoPorArco.get(arcoId);
        
        if (suma == null || conteo == null || conteo == 0) {
            return 0.0;
        }
        
        return suma / conteo;
    }
    
    /**
     * Obtiene todos los IDs de arcos con resultados.
     */
    public Set<String> getArcosConResultados() {
        return sumaVelocidadesPorArco.keySet();
    }
    
    /**
     * Obtiene la suma de velocidades para un arco.
     */
    public double getSumaVelocidades(String arcoId) {
        return sumaVelocidadesPorArco.getOrDefault(arcoId, 0.0);
    }
    
    /**
     * Obtiene el conteo de observaciones para un arco.
     */
    public int getConteo(String arcoId) {
        return conteoPorArco.getOrDefault(arcoId, 0);
    }
    
    /**
     * Total de lotes procesados.
     */
    public long getTotalLotesProcesados() {
        return totalLotesProcesados;
    }
    
    /**
     * Total de resultados recibidos.
     */
    public long getTotalResultadosRecibidos() {
        return totalResultadosRecibidos;
    }
    
    /**
     * Número de arcos únicos con datos.
     */
    public int getNumeroArcosUnicos() {
        return sumaVelocidadesPorArco.size();
    }
    
    /**
     * Alias para estadísticas (usado por ServerApp).
     */
    public int getArcoCount() {
        return sumaVelocidadesPorArco.size();
    }
    
    /**
     * Imprime resumen de estadísticas.
     */
    public void printResumen() {
        System.out.println("\n========== RESUMEN AGGREGATOR ==========");
        System.out.println("Lotes procesados: " + totalLotesProcesados);
        System.out.println("Arcos únicos: " + sumaVelocidadesPorArco.size());
        System.out.println("Resultados totales: " + totalResultadosRecibidos);
        System.out.println("=========================================\n");
    }
}
