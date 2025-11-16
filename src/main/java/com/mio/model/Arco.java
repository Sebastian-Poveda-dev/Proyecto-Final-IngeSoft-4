package com.mio.model;

/**
 * Representa un arco/conexión entre dos paradas consecutivas en una ruta
 */
public class Arco {
    private Stop origen;
    private Stop destino;
    private int lineId;
    private int orientation;
    private int sequenceStart;

    public Arco(Stop origen, Stop destino, int lineId, int orientation, int sequenceStart) {
        this.origen = origen;
        this.destino = destino;
        this.lineId = lineId;
        this.orientation = orientation;
        this.sequenceStart = sequenceStart;
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

    @Override
    public String toString() {
        return String.format("  Arco [Seq: %d]: %s (%d) -> %s (%d)", 
                sequenceStart,
                origen.getShortName(), 
                origen.getStopId(),
                destino.getShortName(), 
                destino.getStopId());
    }
}
