package com.mio.model;

/**
 * Representa la relación entre una línea y una parada
 */
public class LineStop implements Comparable<LineStop> {
    private int lineStopId;
    private int stopSequence;
    private int orientation; // 0 = ida, 1 = vuelta
    private int lineId;
    private int stopId;
    private int planVersionId;
    private int lineVariant;

    public LineStop(int lineStopId, int stopSequence, int orientation, int lineId, 
                    int stopId, int planVersionId, int lineVariant) {
        this.lineStopId = lineStopId;
        this.stopSequence = stopSequence;
        this.orientation = orientation;
        this.lineId = lineId;
        this.stopId = stopId;
        this.planVersionId = planVersionId;
        this.lineVariant = lineVariant;
    }

    public int getLineStopId() {
        return lineStopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getLineId() {
        return lineId;
    }

    public int getStopId() {
        return stopId;
    }

    public int getPlanVersionId() {
        return planVersionId;
    }

    public int getLineVariant() {
        return lineVariant;
    }

    @Override
    public int compareTo(LineStop other) {
        // Ordenar por secuencia
        return Integer.compare(this.stopSequence, other.stopSequence);
    }

    @Override
    public String toString() {
        return "LineStop{" +
                "lineId=" + lineId +
                ", stopId=" + stopId +
                ", sequence=" + stopSequence +
                ", orientation=" + orientation +
                '}';
    }
}
