package com.mio.model;

/**
 * Representa un arco/conexión entre dos paradas consecutivas en una ruta.
 * GRAFO PONDERADO: Cada arco mantiene un peso que representa la velocidad promedio.
 */
public class Arco {
    private Stop origen;
    private Stop destino;
    private int lineId;
    private int orientation;
    private int sequenceStart;
    
    // Peso del arco: velocidad promedio en km/h
    private double velocidadPromedio;
    
    // Para cálculo incremental de la velocidad promedio
    private double sumaVelocidades;
    private int conteoObservaciones;

    public Arco(Stop origen, Stop destino, int lineId, int orientation, int sequenceStart) {
        this.origen = origen;
        this.destino = destino;
        this.lineId = lineId;
        this.orientation = orientation;
        this.sequenceStart = sequenceStart;
        this.velocidadPromedio = 0.0;
        this.sumaVelocidades = 0.0;
        this.conteoObservaciones = 0;
    }

    public Stop getOrigen() {
        return origen;
    }

    public Stop getDestino() {
        return destino;
    }

    public int getLineId() {
        return lineId;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getSequenceStart() {
        return sequenceStart;
    }
    
    public double getVelocidadPromedio() {
        return velocidadPromedio;
    }
    
    public double getSumaVelocidades() {
        return sumaVelocidades;
    }
    
    public int getConteoObservaciones() {
        return conteoObservaciones;
    }
    
    /**
     * Actualiza el peso del arco a partir de una nueva observación de velocidad.
     * Thread-safe para uso concurrente.
     * @param velocidadInstantanea velocidad instantánea calculada en km/h
     */
    public synchronized void agregarObservacion(double velocidadInstantanea) {
        sumaVelocidades += velocidadInstantanea;
        conteoObservaciones++;
        velocidadPromedio = sumaVelocidades / conteoObservaciones;
    }
    
    /**
     * Actualiza el peso del arco a partir de resultados parciales agregados.
     * Usado por el Aggregator del Master cuando recibe datos de Workers.
     * Thread-safe para uso concurrente.
     * @param sumaParcial suma de velocidades del lote procesado
     * @param conteoParcial número de observaciones del lote procesado
     */
    public synchronized void agregarResultadosParciales(double sumaParcial, int conteoParcial) {
        if (conteoParcial > 0) {
            sumaVelocidades += sumaParcial;
            conteoObservaciones += conteoParcial;
            velocidadPromedio = sumaVelocidades / conteoObservaciones;
        }
    }
    
    /**
     * Resetea las estadísticas del arco (útil para reiniciar el análisis)
     */
    public synchronized void resetearEstadisticas() {
        sumaVelocidades = 0.0;
        conteoObservaciones = 0;
        velocidadPromedio = 0.0;
    }
    
    /**
     * Genera un identificador único para este arco basado en lineId, stopId de origen y orientación.
     * Útil para mapear datagramas a arcos.
     * @return String en formato "lineId:stopIdOrigen:orientation"
     */
    public String getArcoId() {
        return String.format("%d:%d:%d", lineId, origen.getStopId(), orientation);
    }
    
    /**
     * Genera un identificador único simplificado usando solo lineId y stopId de origen.
     * @return String en formato "lineId:stopIdOrigen"
     */
    public String getArcoIdSimple() {
        return String.format("%d:%d", lineId, origen.getStopId());
    }

    @Override
    public String toString() {
        return String.format("  Arco [Seq: %d]: %s (%d) -> %s (%d) | Vel.Prom: %.2f km/h (%d obs)",
                sequenceStart,
                origen.getShortName(),
                origen.getStopId(),
                destino.getShortName(),
                destino.getStopId(),
                velocidadPromedio,
                conteoObservaciones);
    }
}