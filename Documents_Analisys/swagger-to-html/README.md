# Generador de Documentación HTML para APIs Swagger

Este generador convierte especificaciones Swagger/OpenAPI en un documento HTML autónomo optimizado para impresión física.

## 📋 Descripción

Procesa las especificaciones Swagger de las APIs `edu-admin` y `edu-mobile` para generar un documento HTML único que incluye:

- **Portada** con estadísticas generales
- **Tabla de contenidos** navegable
- **Documentación detallada** de cada endpoint organizados por módulos
- **Información de parámetros** (nombre, ubicación, tipo, requerido, descripción)
- **CSS optimizado** tanto para pantalla como para impresión

## 🚀 Uso Rápido

### Generar el documento

```bash
cd Documents_Analisys/swagger-to-html
node generator.js
```

O usando npm:

```bash
npm run generate
```

### Resultado

El generador creará el archivo:
```
Documents_Analisys/output/edugo-apis-documentation.html
```

## 📖 Visualizar el Documento

### En el navegador

```bash
open ../output/edugo-apis-documentation.html
```

O simplemente abre el archivo `edugo-apis-documentation.html` con tu navegador favorito:
- Chrome
- Firefox
- Safari
- Edge

### Características de la visualización en pantalla

- ✅ Navegación sticky que siempre está visible
- ✅ Tarjetas de endpoint con hover effects
- ✅ HTTP method badges coloridos (GET verde, POST azul, etc.)
- ✅ Links internos funcionales en la tabla de contenidos
- ✅ Diseño responsive y moderno

## 🖨️ Imprimir el Documento

### Opción 1: Guardar como PDF

1. Abre el archivo HTML en tu navegador
2. Presiona **Ctrl+P** (Windows/Linux) o **Cmd+P** (Mac)
3. En el diálogo de impresión:
   - **Destino:** Selecciona "Guardar como PDF"
   - **Layout:** Portrait (vertical)
   - **Márgenes:** Predeterminados
   - **Opciones:** Activa "Gráficos de fondo"
4. Haz clic en "Guardar" y elige la ubicación

### Opción 2: Imprimir directamente

1. Abre el archivo HTML en tu navegador
2. Presiona **Ctrl+P** (Windows/Linux) o **Cmd+P** (Mac)
3. Selecciona tu impresora
4. Configura:
   - **Papel:** A4 o Letter
   - **Orientación:** Vertical
   - **Márgenes:** Predeterminados
5. Imprime

### Características de impresión

- ✅ **Page breaks estratégicos**: Cada módulo comienza en una página nueva
- ✅ **No división de contenido**: Endpoints y tablas no se dividen entre páginas
- ✅ **Tipografía legible**: Georgia/Times serif de 10pt
- ✅ **Márgenes apropiados**: 2cm arriba/abajo, 1.5cm izq/der
- ✅ **HTTP method badges**: Visibles con backgrounds en grayscale
- ✅ **Tabla de contenidos**: Incluida al inicio del documento
- ✅ **Portada**: Primera página con estadísticas generales

### Estimación de páginas

- **edu-admin**: ~15-18 páginas (33 endpoints)
- **edu-mobile**: ~7-10 páginas (17 endpoints)
- **Total estimado**: **24-30 páginas** (dependiendo de la cantidad de parámetros)

## 📁 Estructura de Archivos

```
Documents_Analisys/
├── swagger-to-html/
│   ├── generator.js       # Script generador (este archivo lo ejecutas)
│   ├── template.html      # Template HTML con CSS inline
│   ├── package.json       # Configuración npm
│   └── README.md          # Este archivo
├── edu-admin/
│   └── swagger.json       # Especificación Swagger de API admin
├── edu-mobile/
│   └── swagger.json       # Especificación Swagger de API mobile
└── output/
    └── edugo-apis-documentation.html  # ✨ Documento generado
```

## 🎨 Personalización

### Modificar el template HTML

Edita [template.html](template.html) para cambiar:
- Estilos CSS (colores, tipografía, márgenes)
- Estructura del documento
- Formato de las tablas

### Modificar la lógica de generación

Edita [generator.js](generator.js) para cambiar:
- Cómo se agrupan los endpoints
- Qué información se muestra
- Formato de los parámetros
- Orden de las secciones

## 🔧 Requisitos

- **Node.js** (cualquier versión reciente, v14+)
- No requiere dependencias externas (solo módulos nativos de Node.js)

## 📊 Información Procesada

El generador extrae y muestra:

### De cada endpoint:
- ✅ Método HTTP (GET, POST, PUT, DELETE, PATCH)
- ✅ Path completo del endpoint
- ✅ Resumen y descripción
- ✅ Parámetros con:
  - Nombre
  - Ubicación (query, path, body, header)
  - Si es requerido
  - Tipo de dato
  - Descripción

### De cada API:
- ✅ Título
- ✅ Versión
- ✅ Host
- ✅ Base Path
- ✅ Descripción general

### Estadísticas:
- ✅ Total de endpoints por API
- ✅ Cantidad de endpoints por módulo
- ✅ Fecha de generación

## 🆘 Solución de Problemas

### Error: "No se encontró swagger.json"

Verifica que los archivos existan en:
- `../edu-admin/swagger.json`
- `../edu-mobile/swagger.json`

### Error: "Cannot parse JSON"

Los archivos Swagger pueden estar corruptos. Verifica que sean JSON válidos:

```bash
node -e "JSON.parse(require('fs').readFileSync('../edu-admin/swagger.json', 'utf8'))"
```

### El HTML no se ve bien al imprimir

Asegúrate de:
1. Usar un navegador moderno (Chrome, Firefox, Safari, Edge)
2. Activar "Gráficos de fondo" en las opciones de impresión
3. Usar márgenes predeterminados
4. No usar "Simplificar página" o modos de lectura

### Las tablas se ven cortadas

Algunos parámetros con descripciones muy largas pueden causar que las tablas sean anchas. Esto es normal. El CSS está optimizado para word-wrap, pero si es necesario, puedes:
1. Reducir el tamaño de fuente en el CSS
2. Usar orientación landscape para esas páginas
3. Editar el template para limitar el ancho de las descripciones

## 📝 Notas

- El documento generado es **completamente autónomo** (no requiere archivos externos)
- Todo el CSS está **inline** en el HTML
- El archivo puede ser **compartido fácilmente** (un solo archivo)
- Compatible con **cualquier navegador moderno**
- Se puede **convertir a PDF** sin pérdida de formato

## 🔄 Regenerar Documentación

Si actualizas los archivos Swagger, simplemente ejecuta de nuevo:

```bash
node generator.js
```

El archivo `edugo-apis-documentation.html` será sobrescrito con la nueva versión.

## 📧 Soporte

Para problemas o sugerencias, contacta al equipo de desarrollo de EduGo.

---

**Generado por:** EduGo Swagger-to-HTML Generator v1.0.0
