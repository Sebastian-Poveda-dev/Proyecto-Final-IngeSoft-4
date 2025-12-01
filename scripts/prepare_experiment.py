#!/usr/bin/env python3
"""
Script para preparar experimentos dividiendo datasets entre múltiples buses.
Uso: python prepare_experiment.py <dataset> <num_buses>
  dataset: small_100k, medium_1m, large_8m
  num_buses: número de buses (default 3)
"""

import sys
import os
from pathlib import Path

DATASETS = {
    "small_100k": "data/experiments/small_100k.csv",
    "medium_1m": "data/experiments/medium_1m.csv", 
    "large_8m": "data/experiments/large_8m.csv"
}

def split_dataset(dataset_name: str, num_buses: int = 3):
    if dataset_name not in DATASETS:
        print(f"ERROR: Dataset '{dataset_name}' no válido.")
        print(f"Opciones: {', '.join(DATASETS.keys())}")
        sys.exit(1)
    
    source_file = DATASETS[dataset_name]
    
    if not os.path.exists(source_file):
        print(f"ERROR: No se encuentra {source_file}")
        sys.exit(1)
    
    print("=" * 60)
    print("       PREPARADOR DE EXPERIMENTOS - Multi-Bus (Python)")
    print("=" * 60)
    print()
    print(f"Dataset: {dataset_name}")
    print(f"Archivo: {source_file}")
    print(f"Buses:   {num_buses}")
    print()
    
    # Leer archivo eficientemente
    print("Leyendo archivo...", end=" ", flush=True)
    with open(source_file, 'r', encoding='utf-8') as f:
        header = f.readline()
        lines = f.readlines()
    
    total_lines = len(lines)
    print(f"OK ({total_lines:,} datagramas)")
    print()
    
    # Calcular líneas por bus
    lines_per_bus = (total_lines + num_buses - 1) // num_buses
    print(f"Datagramas por bus: ~{lines_per_bus:,}")
    print()
    
    # Dividir y escribir a cada bus
    for i in range(1, num_buses + 1):
        bus_dir = f"dist/bus_{i}/data"
        output_file = f"{bus_dir}/datagrams.csv"
        
        if not os.path.exists(bus_dir):
            print(f"ADVERTENCIA: No existe {bus_dir} - saltando bus {i}")
            continue
        
        # Calcular rango
        start_idx = (i - 1) * lines_per_bus
        end_idx = min(start_idx + lines_per_bus, total_lines)
        count = end_idx - start_idx
        
        # Escribir archivo
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(header)
            f.writelines(lines[start_idx:end_idx])
        
        print(f"[OK] Bus {i} : {count:,} datagramas -> {output_file}")
    
    print()
    print("=" * 60)
    print("                 PREPARACION COMPLETADA")
    print("=" * 60)
    print()
    print("Ahora copia las carpetas bus_1, bus_2, bus_3 a las maquinas")
    print("y ejecuta los 3 buses SIMULTANEAMENTE para saturar el sistema.")
    print()
    print("Comandos de ejecucion:")
    print(f'  1. Servidor: java -jar server.jar data/received_datagrams.csv 10000 <numWorkers> "{dataset_name}" "exp_<N>w_{dataset_name}"')
    print("  2. Workers:  java -jar worker.jar (en cada maquina worker)")
    print("  3. Buses:    java -jar bus.jar (ejecutar los 3 AL MISMO TIEMPO)")
    print()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python prepare_experiment.py <dataset> [num_buses]")
        print("  dataset: small_100k, medium_1m, large_8m")
        print("  num_buses: número de buses (default 3)")
        sys.exit(1)
    
    dataset = sys.argv[1]
    num_buses = int(sys.argv[2]) if len(sys.argv) > 2 else 3
    
    split_dataset(dataset, num_buses)
