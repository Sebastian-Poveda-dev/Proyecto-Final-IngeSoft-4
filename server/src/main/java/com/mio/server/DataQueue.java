package com.mio.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

import com.mio.model.Datagram;

/**
 * Cola thread-safe para el patrón Producer-Consumer con lotes.
 * CCOController produce lotes de datagramas (Producer).
 * Master consumirá lotes de datagramas (Consumer).
 */
public class DataQueue {
    private final BlockingQueue<List<Datagram>> queue;
    private final int capacity;
    
    public DataQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    /**
     * Agrega un lote de datagramas a la cola (bloqueante si está llena).
     * @param lote el lote de datagramas a encolar
     * @throws InterruptedException si el thread es interrumpido
     */
    public void enqueueLote(List<Datagram> lote) throws InterruptedException {
        queue.put(lote);
    }
    
    /**
     * Intenta agregar un lote sin bloquear.
     * @param lote el lote de datagramas a encolar
     * @return true si se agregó, false si la cola estaba llena
     */
    public boolean tryEnqueueLote(List<Datagram> lote) {
        return queue.offer(lote);
    }
    
    /**
     * Obtiene y remueve el siguiente lote (bloqueante si está vacía).
     * @return el siguiente lote de datagramas
     * @throws InterruptedException si el thread es interrumpido
     */
    public List<Datagram> dequeueLote() throws InterruptedException {
        return queue.take();
    }
    
    /**
     * Intenta obtener un lote sin bloquear.
     * @return el siguiente lote, o null si está vacía
     */
    public List<Datagram> tryDequeueLote() {
        return queue.poll();
    }
    
    /**
     * Número de lotes en la cola.
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Verifica si la cola está vacía.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Capacidad máxima de la cola (en número de lotes).
     */
    public int getCapacity() {
        return capacity;
    }
}
