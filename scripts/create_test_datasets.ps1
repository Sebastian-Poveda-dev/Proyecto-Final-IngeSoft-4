# create_test_datasets.ps1 - Crear datasets de diferentes tamanios para experimentos
# Uso: .\scripts\create_test_datasets.ps1

param(
    [string]$InputFile = "data/datagrams4streaming.csv",
    [string]$OutputDir = "data/experiments"
)

Write-Host "============================================================"
Write-Host "     CREAR DATASETS PARA EXPERIMENTOS                       "
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

# Tamanios de datasets (escala logaritmica)
$sizes = @(
    @{ Name = "small_100k"; Lines = 100000 },
    @{ Name = "medium_1m"; Lines = 1000000 },
    @{ Name = "large_8m"; Lines = 8000000 }
)

Write-Host "Archivo de entrada: $InputFile"
Write-Host "Directorio salida:  $OutputDir"
Write-Host ""
Write-Host "Datasets a crear:"
foreach ($size in $sizes) {
    Write-Host "  - $($size.Name): $($size.Lines.ToString('N0')) datagramas"
}
Write-Host ""

# Leer header
$reader = [System.IO.StreamReader]::new($InputFile, [System.Text.Encoding]::UTF8)
$header = $reader.ReadLine()
$reader.Close()

foreach ($size in $sizes) {
    $outputFile = Join-Path $OutputDir "$($size.Name).csv"
    $targetLines = $size.Lines
    
    Write-Host "Creando $($size.Name)..."
    
    $reader = [System.IO.StreamReader]::new($InputFile, [System.Text.Encoding]::UTF8)
    $writer = [System.IO.StreamWriter]::new($outputFile, $false, [System.Text.Encoding]::UTF8)
    
    # Escribir header
    $null = $reader.ReadLine() # Skip header del input
    $writer.WriteLine($header)
    
    $count = 0
    while ($count -lt $targetLines -and ($line = $reader.ReadLine()) -ne $null) {
        $writer.WriteLine($line)
        $count++
        
        # Mostrar progreso cada 500,000 lineas
        if ($count % 500000 -eq 0) {
            $pct = [math]::Round(($count / $targetLines) * 100, 1)
            Write-Host "  Progreso: $pct% ($($count.ToString('N0')) / $($targetLines.ToString('N0')))"
        }
    }
    
    $writer.Close()
    $reader.Close()
    
    $fileSize = [math]::Round((Get-Item $outputFile).Length / 1MB, 2)
    Write-Host "  [OK] $($size.Name).csv - $($count.ToString('N0')) datagramas ($fileSize MB)"
    Write-Host ""
}

Write-Host "============================================================"
Write-Host "     DATASETS CREADOS                                       "
Write-Host "============================================================"
Write-Host ""
Write-Host "Archivos en: $OutputDir"
Get-ChildItem $OutputDir -Filter "*.csv" | ForEach-Object {
    $lines = 0
    $reader = [System.IO.StreamReader]::new($_.FullName)
    while ($reader.ReadLine() -ne $null) { $lines++ }
    $reader.Close()
    $lines-- # Restar header
    $size = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  - $($_.Name): $($lines.ToString('N0')) datagramas ($size MB)"
}
Write-Host ""
Write-Host "Para usar en experimentos, copia el dataset deseado a:"
Write-Host "  dist/bus_1/data/datagrams.csv"
