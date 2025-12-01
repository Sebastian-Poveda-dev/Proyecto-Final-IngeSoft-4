package com.mio.server;

import MIO.*;
import com.mio.model.Datagram;
import com.zeroc.Ice.Current;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MasterImpl - Consumer del patrón Producer-Consumer e interfaz ICE para Workers.
 * 
 * Responsabilidades:
 * 1. Consumir lotes de DataQueue
 * 2. Servir lotes a Workers vía ICE (getLote)
 * 3. Recibir resultados parciales de Workers (sendResultadosParciales)
 * 4. Usar Aggregator para combinar resultados
 */
public class MasterImpl implements MasterService {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final DataQueue dataQueue;
    private final Aggregator aggregator;
    private final Set<String> workersRegistrados;
    private final AtomicInteger loteIdCounter;
    
    // Estadísticas
    private long lotesServidos;
    private long lotesVacios;
    private boolean procesamientoActivo;
    
    public MasterImpl(DataQueue dataQueue, Aggregator aggregator) {
        this.dataQueue = dataQueue;
        this.aggregator = aggregator;
        this.workersRegistrados = new HashSet<>();
        this.loteIdCounter = new AtomicInteger(0);
        this.lotesServidos = 0;
        this.lotesVacios = 0;
        this.procesamientoActivo = true;
    }
    
    @Override
    public synchronized LoteDatagram getLote(Current current) {
        LoteDatagram lote = new LoteDatagram();
        
        // Intentar obtener un lote de la cola (non-blocking)
        List<Datagram> datagramas = dataQueue.tryDequeueLote();
        
        if (datagramas == null || datagramas.isEmpty()) {
            // No hay lotes disponibles
            lote.loteId = -1;
            lote.datagramas = new DatagramStruct[0];
            lotesVacios++;
            return lote;
        }
        
        // Convertir List<Datagram> a DatagramStruct[]
        lote.loteId = loteIdCounter.incrementAndGet();
        lote.datagramas = new DatagramStruct[datagramas.size()];
        
        for (int i = 0; i < datagramas.size(); i++) {
            lote.datagramas[i] = toIceStruct(datagramas.get(i));
        }
        
        lotesServidos++;
        
        if (lotesServidos % 5 == 0) {
            System.out.printf("[Master] Lotes servidos: %d | Datagramas: %d | Cola restante: %d\n",
                lotesServidos, datagramas.size(), dataQueue.size());
        }
        
        return lote;
    }
    
    @Override
    public void sendResultadosParciales(ResultadosParciales resultados, Current current) {
        System.out.printf("[Master] Recibidos resultados del Worker '%s' - Lote #%d - %d arcos\n",
            resultados.workerId, resultados.loteId, 
            resultados.resultados != null ? resultados.resultados.length : 0);
        
        // Delegar al Aggregator
        aggregator.agregarResultadosParciales(resultados);
    }
    
    @Override
    public boolean hayMasLotes(Current current) {
        return procesamientoActivo && !dataQueue.isEmpty();
    }
    
    @Override
    public synchronized boolean registrarWorker(String workerId, Current current) {
        if (workersRegistrados.add(workerId)) {
            System.out.println("[Master] Worker registrado: " + workerId);
            return true;
        }
        return false; // Ya estaba registrado
    }
    
    /**
     * Convierte un Datagram Java a DatagramStruct ICE.
     */
    private DatagramStruct toIceStruct(Datagram d) {
        DatagramStruct ice = new DatagramStruct();
        ice.eventType = d.getEventType() != null ? d.getEventType() : "";
        ice.registerDate = d.getRegisterDate() != null ? 
            d.getRegisterDate().format(DATE_FORMATTER) : "";
        ice.stopId = d.getStopId();
        ice.odometer = d.getOdometer();
        ice.latitude = d.getLatitude();
        ice.longitude = d.getLongitude();
        ice.taskId = d.getTaskId();
        ice.lineId = d.getLineId();
        ice.tripId = d.getTripId();
        ice.unknown1 = d.getUnknown1() != null ? d.getUnknown1() : "";
        ice.datagramDate = d.getDatagramDate() != null ? 
            d.getDatagramDate().format(DATE_FORMATTER) : "";
        ice.busId = d.getBusId();
        return ice;
    }
    
    /**
     * Detiene el procesamiento.
     */
    public void stop() {
        procesamientoActivo = false;
    }
    
    /**
     * Número de Workers registrados.
     */
    public int getNumeroWorkers() {
        return workersRegistrados.size();
    }
    
    /**
     * Alias para estadísticas (usado por ServerApp).
     */
    public int getWorkerCount() {
        return workersRegistrados.size();
    }
    
    /**
     * Total de lotes servidos.
     */
    public long getLotesServidos() {
        return lotesServidos;
    }
    
    /**
     * Alias para estadísticas (usado por ServerApp).
     */
    public long getLotesDespachados() {
        return lotesServidos;
    }
    
    /**
     * Total de solicitudes vacías (cola vacía).
     */
    public long getLotesVacios() {
        return lotesVacios;
    }
    
    /**
     * Obtiene el Aggregator.
     */
    public Aggregator getAggregator() {
        return aggregator;
    }
}
