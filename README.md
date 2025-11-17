# Visualización 2D del Grafo - Sistema MIO

### INTEGRANTES: Juan Sebastian Poveda & Santiago Cardenas
> **Note**
> 
> Se observa que el grafo es similar al mapa de la ciudad de Cali, por lo tanto, se presume que es correcto.


## Uso rápido

### Generar visualización del grafo completo

```powershell
.\gradlew.bat visualizar
```

Este comando:
- `output/grafo_completo_MIO.jpg` 
### Ejecución con menú interactivo

```powershell
.\gradlew.bat run
```

Después de mostrar el grafo en consola, el programa preguntará si deseas generar visualizaciones.

#### Opción 1: Grafo completo
Genera una imagen con todas las líneas y paradas del sistema:

- Muestra todas las 105 líneas y 2119 paradas

#### Opción 2: Línea específica
Genera una imagen de una sola línea con todas sus variantes:
- **Archivo**: `output/linea_<nombre>.jpg`
- Ejemplo: `output/linea_A19B.jpg`
- Cada variante se muestra con un color diferente


#### Opción 3: Varias líneas de ejemplo
Genera imágenes de las primeras 5 líneas automáticamente

## Formato de salida

Las imágenes JPG incluyen:

- **Nodos (paradas)**: Círculos blancos con borde negro
- **Arcos (aristas)**: Líneas con flechas indicando dirección
- **Etiquetas**: Código corto de cada parada
- **Leyenda**: Información de variantes (orientación IDA/VUELTA)
- **Colores**: Cada variante tiene un color único 


## Ejemplo de uso completo

```powershell
# 1. Compilar
.\gradlew.bat build

# 2. Ejecutar
.\gradlew.bat run

# 3. Cuando pregunte si desea visualizar, responder: s

# 4. Seleccionar opción:
#    1 - Para grafo completo
#    2 - Para línea específica (ingrese el ID)
#    3 - Para 5 líneas de ejemplo

# 5. Las imágenes se guardarán en la carpeta output/
```





