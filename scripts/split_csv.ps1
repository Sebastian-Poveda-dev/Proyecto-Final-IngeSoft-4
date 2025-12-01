# split_csv.ps1 - Script para dividir CSV grande en N partes
# Uso: .\scripts\split_csv.ps1 -InputFile "data/datagrams4streaming.csv" -Parts 3

param(
    [string]$InputFile = "data/datagrams4streaming.csv",
    [int]$Parts = 3,
    [string]$OutputDir = "data/splits"
)

Write-Host "============================================================"
Write-Host "           SPLIT CSV - Divisor de Archivos                  "
Write-Host "============================================================"
Write-Host ""

# Verificar que existe el archivo
if (-not (Test-Path $InputFile)) {
    Write-Host "ERROR: No se encontro el archivo: $InputFile" -ForegroundColor Red
    exit 1
}

# Crear directorio de salida
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

Write-Host "Archivo de entrada: $InputFile"
Write-Host "Numero de partes:   $Parts"
Write-Host "Directorio salida:  $OutputDir"
Write-Host ""

# Contar lineas totales (eficiente para archivos grandes)
Write-Host "Contando lineas... (puede tomar un momento)"
$totalLineas = 0
$reader = [System.IO.StreamReader]::new($InputFile, [System.Text.Encoding]::UTF8)
$header = $reader.ReadLine()
while ($null -ne $reader.ReadLine()) {
    $totalLineas++
}
$reader.Close()

Write-Host "Total de datagramas: $totalLineas"
$lineasPorParte = [math]::Ceiling($totalLineas / $Parts)
Write-Host "Datagramas por parte: ~$lineasPorParte"
Write-Host ""

# Dividir el archivo
Write-Host "Dividiendo archivo..."
$reader = [System.IO.StreamReader]::new($InputFile, [System.Text.Encoding]::UTF8)
$header = $reader.ReadLine()

$parteActual = 1
$lineasEnParte = 0
$outputFile = Join-Path $OutputDir "split_$parteActual.csv"
$writer = [System.IO.StreamWriter]::new($outputFile, $false, [System.Text.Encoding]::UTF8)
$writer.WriteLine($header)

$lineaActual = 0
while ($null -ne ($linea = $reader.ReadLine())) {
    $lineaActual++
    $lineasEnParte++
    $writer.WriteLine($linea)
    
    # Mostrar progreso cada 100,000 lineas
    if ($lineaActual % 100000 -eq 0) {
        $porcentaje = [math]::Round(($lineaActual / $totalLineas) * 100, 1)
        Write-Host "  Progreso: $porcentaje% ($lineaActual / $totalLineas)"
    }
    
    # Cambiar a siguiente parte si alcanzamos el limite
    if ($lineasEnParte -ge $lineasPorParte -and $parteActual -lt $Parts) {
        $writer.Close()
        Write-Host "  [OK] Creado: split_$parteActual.csv ($lineasEnParte datagramas)"
        
        $parteActual++
        $lineasEnParte = 0
        $outputFile = Join-Path $OutputDir "split_$parteActual.csv"
        $writer = [System.IO.StreamWriter]::new($outputFile, $false, [System.Text.Encoding]::UTF8)
        $writer.WriteLine($header)
    }
}

$writer.Close()
$reader.Close()
Write-Host "  [OK] Creado: split_$parteActual.csv ($lineasEnParte datagramas)"

Write-Host ""
Write-Host "============================================================"
Write-Host "               DIVISION COMPLETADA                          "
Write-Host "============================================================"
Write-Host ""
Write-Host "Archivos creados en: $OutputDir"
Get-ChildItem $OutputDir -Filter "split_*.csv" | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  - $($_.Name) ($size MB)"
}
