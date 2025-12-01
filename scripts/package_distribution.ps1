# package_distribution.ps1 - Script para empaquetar la distribucion
# Uso: .\scripts\package_distribution.ps1 -Buses 3 -Workers 4 -ServerIP "192.168.1.100"

param(
    [int]$Buses = 3,
    [int]$Workers = 4,
    [string]$ServerIP = "localhost",
    [string]$OutputDir = "dist"
)

Write-Host "============================================================"
Write-Host "           PACKAGE DISTRIBUTION - Empaquetador              "
Write-Host "============================================================"
Write-Host ""

# Verificar que existe el build
$serverJar = Get-ChildItem -Path "server/build/libs" -Filter "server-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
$busJar = Get-ChildItem -Path "bus/build/libs" -Filter "bus-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
$workerJar = Get-ChildItem -Path "worker/build/libs" -Filter "worker-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
$clientJar = Get-ChildItem -Path "client/build/libs" -Filter "client-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1

if (-not $serverJar -or -not $busJar -or -not $workerJar -or -not $clientJar) {
    Write-Host "AVISO: No se encontraron todos los JARs. Compilando proyecto..." -ForegroundColor Yellow
    & .\gradlew.bat shadowJar
    
    $serverJar = Get-ChildItem -Path "server/build/libs" -Filter "server-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    $busJar = Get-ChildItem -Path "bus/build/libs" -Filter "bus-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    $workerJar = Get-ChildItem -Path "worker/build/libs" -Filter "worker-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    $clientJar = Get-ChildItem -Path "client/build/libs" -Filter "client-all.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
}

Write-Host "Configuracion:"
Write-Host "  Buses:    $Buses"
Write-Host "  Workers:  $Workers"
Write-Host "  ServerIP: $ServerIP"
Write-Host ""

# Limpiar directorio de salida
if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null

# =====================================================
# 1. SERVIDOR
# =====================================================
Write-Host "[1/4] Creando paquete del SERVIDOR..."
$serverDir = Join-Path $OutputDir "servidor"
New-Item -ItemType Directory -Path $serverDir | Out-Null
New-Item -ItemType Directory -Path "$serverDir/config" | Out-Null
New-Item -ItemType Directory -Path "$serverDir/lib" | Out-Null
New-Item -ItemType Directory -Path "$serverDir/data" | Out-Null

# Copiar JAR
Copy-Item $serverJar.FullName "$serverDir/lib/server.jar"

# Crear network.properties (nombres deben coincidir con NetworkConfig.java)
@"
# Configuracion de red del servidor
SERVER_HOST=$ServerIP
DATA_RECEIVER_PORT=10000
MASTER_SERVICE_PORT=10001
CLIENT_SERVER_PORT=10002
"@ | Out-File "$serverDir/config/network.properties" -Encoding UTF8

# Script de inicio
@"
@echo off
echo Iniciando Servidor MIO...
echo IP: $ServerIP
echo Puertos: 10000 (DataReceiver), 10001 (Master), 10002 (GraphQuery)
echo.
java -jar lib/server.jar
pause
"@ | Out-File "$serverDir/iniciar_servidor.bat" -Encoding ASCII

Write-Host "  [OK] Servidor empaquetado"

# =====================================================
# 2. BUSES
# =====================================================
Write-Host "[2/4] Creando paquetes de BUSES..."
$splitsExist = Test-Path "data/splits"

for ($i = 1; $i -le $Buses; $i++) {
    $busDir = Join-Path $OutputDir "bus_$i"
    New-Item -ItemType Directory -Path $busDir | Out-Null
    New-Item -ItemType Directory -Path "$busDir/config" | Out-Null
    New-Item -ItemType Directory -Path "$busDir/lib" | Out-Null
    New-Item -ItemType Directory -Path "$busDir/data" | Out-Null
    
    # Copiar JAR
    Copy-Item $busJar.FullName "$busDir/lib/bus.jar"
    
    # Copiar datos correspondientes
    if ($splitsExist -and (Test-Path "data/splits/split_$i.csv")) {
        Copy-Item "data/splits/split_$i.csv" "$busDir/data/datagrams.csv"
    }
    
    # Crear network.properties (nombres deben coincidir con NetworkConfig.java)
    @"
# Configuracion de red del bus $i
BUS_TARGET_SERVER=$ServerIP
BUS_TARGET_PORT=10000
"@ | Out-File "$busDir/config/network.properties" -Encoding UTF8

    # Script de inicio
    @"
@echo off
echo Iniciando Bus $i...
echo Conectando a: $ServerIP`:10000
echo.
java -jar lib/bus.jar data/datagrams.csv
pause
"@ | Out-File "$busDir/iniciar_bus.bat" -Encoding ASCII

    Write-Host "  [OK] Bus $i empaquetado"
}

# =====================================================
# 3. WORKERS
# =====================================================
Write-Host "[3/4] Creando paquetes de WORKERS..."
for ($i = 1; $i -le $Workers; $i++) {
    $workerDir = Join-Path $OutputDir "worker_$i"
    New-Item -ItemType Directory -Path $workerDir | Out-Null
    New-Item -ItemType Directory -Path "$workerDir/config" | Out-Null
    New-Item -ItemType Directory -Path "$workerDir/lib" | Out-Null
    
    # Copiar JAR
    Copy-Item $workerJar.FullName "$workerDir/lib/worker.jar"
    
    # Crear network.properties (nombres deben coincidir con NetworkConfig.java)
    @"
# Configuracion de red del worker $i
WORKER_MASTER_HOST=$ServerIP
WORKER_MASTER_PORT=10001
"@ | Out-File "$workerDir/config/network.properties" -Encoding UTF8

    # Script de inicio
    @"
@echo off
echo Iniciando Worker $i...
echo Conectando a Master: $ServerIP`:10001
echo.
java -jar lib/worker.jar
pause
"@ | Out-File "$workerDir/iniciar_worker.bat" -Encoding ASCII

    Write-Host "  [OK] Worker $i empaquetado"
}

# =====================================================
# 4. CLIENTE
# =====================================================
Write-Host "[4/4] Creando paquete del CLIENTE..."
$clientDir = Join-Path $OutputDir "cliente"
New-Item -ItemType Directory -Path $clientDir | Out-Null
New-Item -ItemType Directory -Path "$clientDir/config" | Out-Null
New-Item -ItemType Directory -Path "$clientDir/lib" | Out-Null

# Copiar JAR
Copy-Item $clientJar.FullName "$clientDir/lib/client.jar"

# Crear network.properties (nombres deben coincidir con NetworkConfig.java)
@"
# Configuracion de red del cliente
CLIENT_SERVER_HOST=$ServerIP
CLIENT_SERVER_PORT=10002
"@ | Out-File "$clientDir/config/network.properties" -Encoding UTF8

# Script de inicio
@"
@echo off
echo Iniciando Cliente MIO...
echo Conectando a: $ServerIP`:10002
echo.
java -jar lib/client.jar
pause
"@ | Out-File "$clientDir/iniciar_cliente.bat" -Encoding ASCII

Write-Host "  [OK] Cliente empaquetado"

# =====================================================
# README
# =====================================================
@"
=======================================================
  SISTEMA DISTRIBUIDO MIO - GUIA DE DESPLIEGUE
=======================================================

ARQUITECTURA:
  Bus(1..N) --> DataReceiver --> CCOController --> Master --> Workers(1..M)
                                                      |
                                                      v
                                               Aggregator
                                                      |
                                                      v
                                              GraphQueryService <-- Cliente

ORDEN DE INICIO:
  1. Servidor   (iniciar_servidor.bat)
  2. Workers    (iniciar_worker.bat en cada maquina)
  3. Buses      (iniciar_bus.bat en cada maquina)
  4. Cliente    (iniciar_cliente.bat para consultas)

PUERTOS:
  - 10000: DataReceiver (recibe datagramas de buses)
  - 10001: MasterService (workers solicitan lotes)
  - 10002: GraphQueryService (cliente hace consultas)

CONFIGURACION:
  Cada carpeta tiene config/network.properties
  Modificar server.host o master.host segun la IP del servidor

REQUISITOS:
  - Java 11 o superior
  - Red: Puertos 10000, 10001, 10002 abiertos

=======================================================
"@ | Out-File "$OutputDir/README.txt" -Encoding UTF8

Write-Host ""
Write-Host "============================================================"
Write-Host "               EMPAQUETADO COMPLETADO                       "
Write-Host "============================================================"
Write-Host ""
Write-Host "Distribucion creada en: $OutputDir/"
Write-Host ""
Write-Host "Estructura:"
Write-Host "  dist/"
Write-Host "  +-- servidor/        -> Maquina central"
for ($i = 1; $i -le $Buses; $i++) {
    Write-Host "  +-- bus_$i/           -> Maquina bus $i"
}
for ($i = 1; $i -le $Workers; $i++) {
    Write-Host "  +-- worker_$i/        -> Maquina worker $i"
}
Write-Host "  +-- cliente/         -> Cualquier maquina"
Write-Host "  +-- README.txt       -> Instrucciones"
Write-Host ""
Write-Host "Para desplegar:"
Write-Host "  1. Copiar 'servidor/' a la maquina central"
Write-Host "  2. Copiar 'bus_N/' a cada maquina con datos"
Write-Host "  3. Copiar 'worker_N/' a cada maquina de procesamiento"
Write-Host "  4. Copiar 'cliente/' donde se haran consultas"
