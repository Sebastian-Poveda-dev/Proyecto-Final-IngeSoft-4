# Script para preparar un experimento dividiendo el dataset entre múltiples buses
# Uso: .\scripts\prepare_experiment.ps1 -Dataset "small_100k" -NumBuses 3

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("small_100k", "medium_1m", "large_8m")]
    [string]$Dataset,
    
    [int]$NumBuses = 3
)

# Mapeo de nombres a archivos
$datasetFiles = @{
    "small_100k" = "data\experiments\small_100k.csv"
    "medium_1m" = "data\experiments\medium_1m.csv"
    "large_8m" = "data\experiments\large_8m.csv"
}

$sourceFile = $datasetFiles[$Dataset]

if (-not (Test-Path $sourceFile)) {
    Write-Host "ERROR: No se encuentra el archivo $sourceFile" -ForegroundColor Red
    exit 1
}

Write-Host "============================================================"
Write-Host "       PREPARADOR DE EXPERIMENTOS - Multi-Bus"
Write-Host "============================================================"
Write-Host ""
Write-Host "Dataset: $Dataset"
Write-Host "Archivo: $sourceFile"
Write-Host "Buses:   $NumBuses"
Write-Host ""

# Contar líneas totales (sin header)
$allLines = Get-Content $sourceFile
$header = $allLines[0]
$dataLines = $allLines | Select-Object -Skip 1
$totalLines = $dataLines.Count

Write-Host "Total datagramas: $totalLines"
Write-Host ""

# Calcular líneas por bus
$linesPerBus = [math]::Ceiling($totalLines / $NumBuses)

Write-Host "Datagramas por bus: ~$linesPerBus"
Write-Host ""

# Dividir y copiar a cada bus
for ($i = 1; $i -le $NumBuses; $i++) {
    $busDir = "dist\bus_$i\data"
    $outputFile = "$busDir\datagrams.csv"
    
    if (-not (Test-Path $busDir)) {
        Write-Host "ADVERTENCIA: No existe $busDir - saltando bus $i" -ForegroundColor Yellow
        continue
    }
    
    # Calcular rango de líneas para este bus
    $startIndex = ($i - 1) * $linesPerBus
    $endIndex = [math]::Min($startIndex + $linesPerBus - 1, $totalLines - 1)
    $count = $endIndex - $startIndex + 1
    
    # Extraer las líneas para este bus
    $busData = $dataLines | Select-Object -Skip $startIndex -First $count
    
    # Escribir archivo con header
    $header | Set-Content $outputFile
    $busData | Add-Content $outputFile
    
    Write-Host "[OK] Bus $i : $count datagramas -> $outputFile" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================"
Write-Host "                 PREPARACION COMPLETADA"
Write-Host "============================================================"
Write-Host ""
Write-Host "Ahora copia las carpetas bus_1, bus_2, bus_3 a las maquinas"
Write-Host "y ejecuta los 3 buses SIMULTANEAMENTE para saturar el sistema."
Write-Host ""
Write-Host "Comandos de ejecucion:"
Write-Host "  1. Servidor: java -jar server.jar data/received_datagrams.csv 10000 <numWorkers> `"$Dataset`" `"exp_<N>w_$Dataset`""
Write-Host "  2. Workers:  java -jar worker.jar (en cada maquina worker)"
Write-Host "  3. Buses:    java -jar bus.jar (ejecutar los 3 AL MISMO TIEMPO)"
Write-Host ""
