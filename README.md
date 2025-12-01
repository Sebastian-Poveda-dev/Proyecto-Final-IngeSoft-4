# Sistema Distribuido MIO - Procesamiento de Datagramas

Sistema distribuido para procesar datagramas del transporte MIO usando ZeroC ICE.

## Arquitectura

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│  BUS 1  │     │  BUS 2  │     │  BUS 3  │
└────┬────┘     └────┬────┘     └────┬────┘
     └───────────────┼───────────────┘
                     │ (datagramas)
                     ▼
              ┌─────────────┐
              │  SERVIDOR   │ :10000 (DataReceiver)
              │  - Recibe   │ :10001 (MasterService)
              │  - Agrupa   │ :10002 (GraphQueryService)
              └──────┬──────┘
                     │ (lotes de 10K)
     ┌───────────────┼───────────────┐
     ▼               ▼               ▼
┌─────────┐     ┌─────────┐     ┌─────────┐
│ WORKER1 │     │ WORKER2 │     │ WORKER N│
└─────────┘     └─────────┘     └─────────┘
```

## Preparación

### 1. Compilar
```powershell
.\gradlew.bat shadowJar
```

### 2. Empaquetar
```powershell
.\scripts\package_distribution.ps1 -ServerIP "192.168.131.106"
```

Crea `dist/` con: `servidor/`, `bus_1-3/`, `worker_1-4/`, `cliente/`

### 3. Preparar dataset
```powershell
python scripts\prepare_experiment.py <dataset> <num_buses>
```

Datasets: `small_100k`, `medium_1m`, `large_8m`

Ejemplo:
```powershell
python scripts\prepare_experiment.py large_8m 3
```

## Ejecución

**Orden:** Servidor → Workers → Buses

### SERVIDOR
```bash
java -jar server.jar <dataFile> <batchSize> <numWorkers> <datasetSize> <experimentId>
```

Ejemplo:
```bash
java -jar server.jar data/received_datagrams.csv 10000 4 "8M" "exp_4w_8m"
```

**Entre experimentos:** `rm data/received_datagrams.csv`

### WORKERS
```bash
java -jar worker.jar
```

### BUSES (ejecutar los 3 al mismo tiempo)
```bash
java -jar bus.jar
```

### CLIENTE (consultas)
```bash
java -jar client.jar
```

## Parámetros del Servidor

| Parámetro | Descripción | Default |
|-----------|-------------|---------|
| dataFile | Archivo de datagramas | `data/received_datagrams.csv` |
| batchSize | Datagramas por lote | `10000` |
| numWorkers | Cantidad de workers | `4` |
| datasetSize | Etiqueta (para logs) | `"unknown"` |
| experimentId | ID experimento | auto |

## Experimentos

| Workers | Comando |
|---------|---------|
| 1 | `java -jar server.jar data/received_datagrams.csv 10000 1 "8M" "exp_1w_8m"` |
| 2 | `java -jar server.jar data/received_datagrams.csv 10000 2 "8M" "exp_2w_8m"` |
| 4 | `java -jar server.jar data/received_datagrams.csv 10000 4 "8M" "exp_4w_8m"` |

Resultados en: `data/experiment_results.csv`

## Cambiar Dataset Original

El dataset original está en `data/datagrams4streaming.csv`. Para usar uno nuevo:

### 1. Reemplazar el archivo fuente
```powershell
# Copiar tu nuevo archivo CSV (debe tener el mismo formato de 12 columnas)
Copy-Item "tu_nuevo_dataset.csv" -Destination "data/datagrams4streaming.csv"
```

### 2. Regenerar datasets de experimentos
```powershell
# Esto crea small_100k, medium_1m, large_8m a partir del nuevo archivo
.\scripts\create_test_datasets.ps1
```

### 3. Preparar para los buses
```powershell
python scripts\prepare_experiment.py large_8m 3
```

**Formato requerido del CSV** (12 columnas):
```
eventType,registerdate,stopId,odometer,latitude,longitude,taskId,lineId,tripId,unknown1,datagramDate,busId
```
