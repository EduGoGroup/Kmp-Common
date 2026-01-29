---
name: 083-audit-step3-execute
description: PASO 3 - Ejecuta mejoras planificadas y genera informe final
allowed-tools: Task, TodoWrite, Read, Write, Edit, Glob, Grep
---

# Auditoría de Comando Slash - PASO 3: Ejecución

⚠️ **ADVERTENCIA**: Este comando MODIFICA archivos reales. Se crean backups automáticamente.

**Prerrequisitos**: Ejecutar PASO 1 y PASO 2 primero.

## Input

```
/083-audit-step3-execute <nombre-comando>
```

Ejemplo: `/083-audit-step3-execute 011-constitution-create-project`

## Output

**Carpeta:** `analisis_comando_slash/<nombre-comando>/ejecucion/`

**Archivos generados:**
- `backups/` - Copias de seguridad de archivos originales
- `01_mejoras_aplicadas.json` - Registro de mejoras exitosas
- `02_mejoras_fallidas.json` - Registro de mejoras que fallaron (si hay)
- `03_informe_final.md` - Informe completo de la ejecución

---

## Estructura de Entrada (desde PASO 2)

El PASO 3 lee el archivo `03_plan_consolidado.json` que tiene esta estructura:

```json
{
  "plan_mejoras": {
    "total_mejoras": 39,
    "por_prioridad": { "CRITICA": 5, "ALTA": 12, "MEDIA": 15, "BAJA": 7 }
  },
  "fases_ejecucion": [
    { "fase": 1, "nombre": "Correcciones Críticas", "mejoras": ["M001", "M002", ...] },
    { "fase": 2, "nombre": "Alta Prioridad - Comando", "mejoras": ["M004", ...] },
    ...
  ],
  "mejoras_comando": [
    {
      "id": "M001",
      "prioridad": "CRITICA",
      "archivo": ".claude/commands/011-constitution-create-project.md",
      "seccion": "frontmatter (allowed-tools)",
      "problema": "El comando usa mcp__MCPEco__get_document pero no está declarada",
      "cambio": "Agregar a allowed-tools: mcp__MCPEco__get_document, MCPSearch, TodoWrite",
      "justificacion": "Best practices: TODAS las herramientas deben declararse"
    }
  ],
  "mejoras_agentes": [
    {
      "id": "DF001",
      "prioridad": "CRITICA",
      "archivo": ".claude/agents/constitution/document-finder-agent.md",
      "seccion": "frontmatter",
      "problema": "Falta subagent_type",
      "cambio": "Agregar: subagent_type: document-finder-agent",
      "justificacion": "Requerido para que Task() funcione"
    }
  ]
}
```

---

## Flujo de Ejecución

### FASE -1: Inicializar TODO List

```typescript
await TodoWrite({
  todos: [
    { content: "Cargar plan de mejoras", status: "pending", activeForm: "Cargando plan" },
    { content: "Crear backups", status: "pending", activeForm: "Creando backups" },
    { content: "Ejecutar mejoras CRÍTICAS", status: "pending", activeForm: "Aplicando críticas" },
    { content: "Ejecutar mejoras ALTAS", status: "pending", activeForm: "Aplicando altas" },
    { content: "Ejecutar mejoras MEDIAS", status: "pending", activeForm: "Aplicando medias" },
    { content: "Ejecutar mejoras BAJAS", status: "pending", activeForm: "Aplicando bajas" },
    { content: "Verificar cambios", status: "pending", activeForm: "Verificando cambios" },
    { content: "Generar informe final", status: "pending", activeForm: "Generando informe" }
  ]
})
```

### FASE 0: Cargar Plan de Mejoras

```typescript
const COMANDO = $ARGUMENTS.trim()
const PLAN_PATH = `analisis_comando_slash/${COMANDO}/procesos_a_mejorar/03_plan_consolidado.json`

// Leer el plan consolidado
const planContent = await Read({ file_path: PLAN_PATH })
const plan = JSON.parse(planContent)

// Validar estructura
if (!plan.mejoras_comando || !plan.mejoras_agentes) {
  throw new Error('Plan inválido: faltan mejoras_comando o mejoras_agentes')
}

const totalMejoras = plan.plan_mejoras.total_mejoras
console.log(`📋 Plan cargado: ${totalMejoras} mejoras a aplicar`)
```

### FASE 1: Crear Backups

```typescript
const OUTPUT_PATH = `analisis_comando_slash/${COMANDO}/ejecucion`
const BACKUP_PATH = `${OUTPUT_PATH}/backups`

// Crear carpeta de backups
await Task({
  subagent_type: "general-purpose",
  description: "Crear backups",
  prompt: `Crea backups de todos los archivos que se van a modificar.
  
  Archivos a respaldar (de plan.plan_mejoras.archivos_afectados):
  ${JSON.stringify(plan.plan_mejoras.archivos_afectados)}
  
  Para cada archivo:
  1. Lee el contenido original con Read
  2. Guarda en ${BACKUP_PATH}/<nombre-archivo>.backup.md con Write
  
  Retorna JSON con lista de backups creados.`
})
```

### FASE 2: Ejecutar Mejoras por Prioridad

**IMPORTANTE**: Ejecutar en orden de prioridad (CRITICA → ALTA → MEDIA → BAJA)

```typescript
const mejorasAplicadas = []
const mejorasFallidas = []

// Combinar todas las mejoras
const todasMejoras = [...plan.mejoras_comando, ...plan.mejoras_agentes]

// Ordenar por prioridad
const ordenPrioridad = { "CRITICA": 1, "ALTA": 2, "MEDIA": 3, "BAJA": 4 }
todasMejoras.sort((a, b) => ordenPrioridad[a.prioridad] - ordenPrioridad[b.prioridad])

// Ejecutar cada mejora
for (const mejora of todasMejoras) {
  console.log(`\n🔧 [${mejora.id}] ${mejora.prioridad}: ${mejora.seccion}`)
  
  try {
    const resultado = await Task({
      subagent_type: "general-purpose",
      description: `Aplicar ${mejora.id}`,
      prompt: `Aplica esta mejora específica:
      
      **ID**: ${mejora.id}
      **Archivo**: ${mejora.archivo}
      **Sección**: ${mejora.seccion}
      **Problema**: ${mejora.problema}
      **Cambio a realizar**: ${mejora.cambio}
      
      INSTRUCCIONES:
      1. Lee el archivo con Read
      2. Localiza la sección "${mejora.seccion}"
      3. Aplica el cambio: "${mejora.cambio}"
      4. Guarda con Edit (NO Write completo)
      5. Verifica que el cambio se aplicó
      
      Retorna JSON:
      {
        "success": true/false,
        "id": "${mejora.id}",
        "archivo": "${mejora.archivo}",
        "cambio_aplicado": "descripción del cambio realizado",
        "error": "mensaje si falló"
      }`
    })
    
    if (resultado.success) {
      mejorasAplicadas.push({ ...mejora, resultado: resultado.cambio_aplicado })
      console.log(`   ✅ Aplicada`)
    } else {
      mejorasFallidas.push({ ...mejora, error: resultado.error })
      console.log(`   ❌ Falló: ${resultado.error}`)
    }
  } catch (error) {
    mejorasFallidas.push({ ...mejora, error: error.message })
    console.log(`   ❌ Error: ${error.message}`)
  }
}
```

### FASE 3: Verificar Cambios

```typescript
await Task({
  subagent_type: "general-purpose",
  description: "Verificar cambios aplicados",
  prompt: `Verifica que las mejoras se aplicaron correctamente.
  
  Para cada mejora aplicada:
  1. Lee el archivo modificado
  2. Busca evidencia del cambio en la sección indicada
  3. Confirma que el cambio está presente
  
  Mejoras a verificar:
  ${JSON.stringify(mejorasAplicadas.map(m => ({ id: m.id, archivo: m.archivo, seccion: m.seccion })))}
  
  Retorna JSON con:
  {
    "verificadas": [{ "id": "M001", "presente": true/false }],
    "total_verificadas": N,
    "total_confirmadas": N
  }`
})
```

### FASE 4: Guardar Registros

```typescript
// Guardar mejoras aplicadas
await Write({
  file_path: `${OUTPUT_PATH}/01_mejoras_aplicadas.json`,
  content: JSON.stringify({
    fecha: new Date().toISOString(),
    comando: COMANDO,
    total_aplicadas: mejorasAplicadas.length,
    mejoras: mejorasAplicadas
  }, null, 2)
})

// Guardar mejoras fallidas (si hay)
if (mejorasFallidas.length > 0) {
  await Write({
    file_path: `${OUTPUT_PATH}/02_mejoras_fallidas.json`,
    content: JSON.stringify({
      fecha: new Date().toISOString(),
      comando: COMANDO,
      total_fallidas: mejorasFallidas.length,
      mejoras: mejorasFallidas
    }, null, 2)
  })
}
```

### FASE 5: Generar Informe Final

```typescript
const informe = `# Informe de Ejecución - Auditoría PASO 3

## Comando: ${COMANDO}
## Fecha: ${new Date().toISOString()}

---

## Resumen

| Métrica | Valor |
|---------|-------|
| **Total mejoras planificadas** | ${totalMejoras} |
| **Mejoras aplicadas** | ${mejorasAplicadas.length} ✅ |
| **Mejoras fallidas** | ${mejorasFallidas.length} ❌ |
| **Tasa de éxito** | ${Math.round(mejorasAplicadas.length / totalMejoras * 100)}% |

---

## Mejoras Aplicadas por Prioridad

### CRÍTICAS (${mejorasAplicadas.filter(m => m.prioridad === 'CRITICA').length})
${mejorasAplicadas.filter(m => m.prioridad === 'CRITICA').map(m => `- [${m.id}] ${m.seccion}: ${m.resultado}`).join('\n')}

### ALTAS (${mejorasAplicadas.filter(m => m.prioridad === 'ALTA').length})
${mejorasAplicadas.filter(m => m.prioridad === 'ALTA').map(m => `- [${m.id}] ${m.seccion}: ${m.resultado}`).join('\n')}

### MEDIAS (${mejorasAplicadas.filter(m => m.prioridad === 'MEDIA').length})
${mejorasAplicadas.filter(m => m.prioridad === 'MEDIA').map(m => `- [${m.id}] ${m.seccion}: ${m.resultado}`).join('\n')}

### BAJAS (${mejorasAplicadas.filter(m => m.prioridad === 'BAJA').length})
${mejorasAplicadas.filter(m => m.prioridad === 'BAJA').map(m => `- [${m.id}] ${m.seccion}: ${m.resultado}`).join('\n')}

---

## Mejoras Fallidas

${mejorasFallidas.length === 0 ? 'Ninguna ✅' : mejorasFallidas.map(m => `- [${m.id}] ${m.archivo} - ${m.seccion}: ${m.error}`).join('\n')}

---

## Archivos Modificados

${plan.plan_mejoras.archivos_afectados.map(a => `- ${a}`).join('\n')}

---

## Backups

Los archivos originales están respaldados en:
\`analisis_comando_slash/${COMANDO}/ejecucion/backups/\`

Para revertir cambios:
\`\`\`bash
cp backups/<archivo>.backup.md <ruta-original>
\`\`\`

---

## Verificación Manual Recomendada

1. Revisar el comando: \`.claude/commands/${COMANDO}.md\`
2. Probar ejecución: \`/${COMANDO} <args>\`
3. Verificar que no hay errores de sintaxis en YAML frontmatter

---

## Fin de Auditoría

✅ Auditoría completada para \`${COMANDO}\`
`

await Write({
  file_path: `${OUTPUT_PATH}/03_informe_final.md`,
  content: informe
})

console.log('\n' + '═'.repeat(60))
console.log('✅ AUDITORÍA PASO 3 COMPLETADA')
console.log(`📁 Resultados en: ${OUTPUT_PATH}`)
console.log(`📊 Mejoras aplicadas: ${mejorasAplicadas.length}/${totalMejoras}`)
console.log('═'.repeat(60))
```

---

## Manejo de Errores

### Si una mejora falla:

1. **Registrar el error** en `mejorasFallidas`
2. **Continuar** con la siguiente mejora (no detener todo el proceso)
3. **Reportar** al final todas las fallidas

### Si el plan no existe:

```typescript
if (!planContent) {
  return {
    success: false,
    error: `No se encontró el plan en ${PLAN_PATH}`,
    suggestion: "Ejecutar primero: /082-audit-step2-plan " + COMANDO
  }
}
```

### Si el backup falla:

```typescript
// CRÍTICO: No continuar sin backup
if (!backupResult.success) {
  throw new Error(`No se pudo crear backup de ${archivo}. Abortando por seguridad.`)
}
```

---

## Rollback (Revertir Cambios)

Si necesitas revertir todos los cambios:

```bash
# Desde la carpeta del proyecto
for file in analisis_comando_slash/<comando>/ejecucion/backups/*.backup.md; do
  original=$(echo $file | sed 's/.backup.md//' | xargs basename)
  # Copiar backup a ubicación original
done
```

O manualmente:
1. Ir a `analisis_comando_slash/<comando>/ejecucion/backups/`
2. Copiar cada `.backup.md` a su ubicación original

---

## Notas Importantes

1. **Orden de ejecución**: CRITICA → ALTA → MEDIA → BAJA
2. **No ejecutar en paralelo**: Las mejoras pueden tener dependencias
3. **Verificar después**: Siempre revisar manualmente los archivos modificados
4. **Backups**: Se crean automáticamente antes de cualquier modificación

---

## Fin del Flujo de Auditoría

Este es el último paso del flujo de auditoría de 3 pasos:
1. `/081-audit-step1-analyze` - Analiza estructura y procesos
2. `/082-audit-step2-plan` - Planifica mejoras detalladas
3. `/083-audit-step3-execute` - Ejecuta mejoras (este paso)
