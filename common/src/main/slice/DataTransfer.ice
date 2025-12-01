// DataTransfer.ice - Definiciones Slice para comunicación Bus → Server y Master → Worker

module MIO {

    /**
     * Estructura que representa un datagrama de bus.
     * Corresponde a los campos del CSV datagrams4streaming.
     */
    struct DatagramStruct {
        string eventType;
        string registerDate;      // Formato: "yyyy-MM-dd HH:mm:ss"
        int stopId;
        double odometer;
        double latitude;
        double longitude;
        int taskId;
        int lineId;
        int tripId;
        string unknown1;
        string datagramDate;      // Formato: "yyyy-MM-dd HH:mm:ss"
        int busId;
    }
    
    /**
     * Secuencia de datagramas para envío de lotes.
     */
    sequence<DatagramStruct> DatagramSeq;
    
    /**
     * Lote de datagramas para procesamiento por Workers.
     */
    struct LoteDatagram {
        int loteId;               // Identificador único del lote
        DatagramSeq datagramas;   // Lista de datagramas en el lote
    }
    
    /**
     * Resultado parcial de un arco procesado por un Worker.
     */
    struct ArcoResult {
        string arcoId;            // Formato: "lineId:stopIdOrigen:orientation"
        double sumaVelocidades;   // Suma de velocidades calculadas
        int conteo;               // Número de observaciones
    }
    
    /**
     * Secuencia de resultados de arcos.
     */
    sequence<ArcoResult> ArcoResultSeq;
    
    /**
     * Resultados parciales enviados por un Worker al Master.
     */
    struct ResultadosParciales {
        int loteId;               // ID del lote procesado
        string workerId;          // Identificador del Worker
        ArcoResultSeq resultados; // Resultados por arco
    }

    /**
     * Interfaz para recibir datagramas desde los buses.
     * Implementada por el servidor (DataReceiverImpl).
     */
    interface DataReceiver {
        /**
         * Envía un datagrama al servidor.
         * @param datagram El datagrama a procesar
         * @return true si fue recibido correctamente
         */
        bool sendDatagram(DatagramStruct datagram);

        /**
         * Permite al bus verificar conectividad con el servidor.
         * @return true si el servidor está activo
         */
        bool ping();
    }
    
    /**
     * Interfaz del Master para comunicación con Workers.
     * Implementada por el servidor (MasterImpl).
     */
    interface MasterService {
        /**
         * Solicita un lote de datagramas para procesar.
         * @return LoteDatagram con el siguiente lote, o lote vacío si no hay más trabajo
         */
        LoteDatagram getLote();
        
        /**
         * Envía resultados parciales al Master.
         * @param resultados Los resultados del procesamiento de un lote
         */
        void sendResultadosParciales(ResultadosParciales resultados);
        
        /**
         * Verifica si hay más lotes disponibles.
         * @return true si hay lotes en cola o pendientes
         */
        bool hayMasLotes();
        
        /**
         * Permite al Worker registrarse con el Master.
         * @param workerId Identificador único del Worker
         * @return true si el registro fue exitoso
         */
        bool registrarWorker(string workerId);
    }
    
    // =====================================================================
    // FASE 6: GraphQueryService - Consultas al grafo desde clientes
    // =====================================================================
    
    /**
     * Información de un arco con su velocidad promedio.
     */
    struct ArcoInfo {
        string arcoId;            // Formato: "lineId-stopOrigen-stopDestino"
        int lineId;               // ID de la línea
        int stopOrigen;           // Parada origen
        int stopDestino;          // Parada destino
        double velocidadPromedio; // Velocidad promedio calculada (km/h)
        int observaciones;        // Número de observaciones
    }
    
    /**
     * Secuencia de información de arcos.
     */
    sequence<ArcoInfo> ArcoInfoSeq;
    
    /**
     * Información resumida de una línea.
     */
    struct LineaInfo {
        int lineId;               // ID de la línea
        string nombre;            // Nombre de la línea (si disponible)
        int cantidadArcos;        // Número de arcos en la línea
        double velocidadPromedio; // Velocidad promedio de la línea
    }
    
    /**
     * Secuencia de información de líneas.
     */
    sequence<LineaInfo> LineaInfoSeq;
    
    /**
     * Interfaz para consultas al grafo desde clientes.
     * Permite obtener información sobre velocidades promedio por arco y línea.
     */
    interface GraphQueryService {
        /**
         * Obtiene la velocidad promedio de un arco específico.
         * @param arcoId ID del arco (formato: "lineId-stopOrigen-stopDestino")
         * @return velocidad promedio en km/h, o -1 si no existe
         */
        double getVelocidadPromedioArco(string arcoId);
        
        /**
         * Obtiene información detallada de un arco.
         * @param arcoId ID del arco
         * @return ArcoInfo con detalles del arco
         */
        ArcoInfo getArcoInfo(string arcoId);
        
        /**
         * Obtiene todas las velocidades de arcos de una línea específica.
         * @param lineId ID de la línea
         * @return secuencia de ArcoInfo de la línea
         */
        ArcoInfoSeq getVelocidadesPorLinea(int lineId);
        
        /**
         * Obtiene la velocidad promedio de todos los arcos de una línea.
         * @param lineId ID de la línea
         * @return velocidad promedio de la línea en km/h
         */
        double getVelocidadPromedioLinea(int lineId);
        
        /**
         * Obtiene lista de todas las líneas con sus estadísticas.
         * @return secuencia de LineaInfo
         */
        LineaInfoSeq getTodasLasLineas();
        
        /**
         * Obtiene los arcos con velocidad más baja (posibles puntos de congestión).
         * @param limite número máximo de arcos a retornar
         * @return secuencia de ArcoInfo ordenados por velocidad ascendente
         */
        ArcoInfoSeq getArcosMasLentos(int limite);
        
        /**
         * Obtiene los arcos con velocidad más alta.
         * @param limite número máximo de arcos a retornar
         * @return secuencia de ArcoInfo ordenados por velocidad descendente
         */
        ArcoInfoSeq getArcosMasRapidos(int limite);
        
        /**
         * Obtiene estadísticas generales del sistema.
         * @return string con resumen de estadísticas
         */
        string getEstadisticasGenerales();
        
        /**
         * Verifica conectividad con el servicio.
         * @return true si el servicio está activo
         */
        bool ping();
    }
}