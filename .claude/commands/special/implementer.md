---
name: implementer
description: Implementar una task del workflow
---

# Implementer: Ejecutar Task

Implementa el codigo de una task especifica del workflow.

---

## Configuracion del Proyecto

```
PROJECT_ID: proj-1769654677209332000
PROJECT_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common
DOCS_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/Documents_Analisys
```

---

## Input

**Argumento**: `$ARGUMENTS` = task_id

**Uso**:
```bash
/special:implementer task-configurar-gradle-wrapper-8-11-con-scripts-ejecuta-e58d92f0
```

---

## Que se espera

1. Leer la informacion de la task usando el PROJECT_ID
2. Validar el codigo actual del proyecto
3. Si necesitas informacion del backend, consulta DOCS_PATH (swagger, APIs, etc.)
4. La implementacion debe ser consistente con el codigo existente
5. Si considera que la historia dada no coincide con los puntos anteriores, informar al usuario antes de seguir
6. Debe compilar - si no compila o no pasan los tests, informar al usuario para decidir pasos a seguir
7. Al terminar exitosamente, pasar la tarea al siguiente paso
8. Crear un work_item con el resultado

---

## Guia de referencia

Adaptate segun el contexto usando como referencia:
`/Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/.claude/commands/041-implementer-task.md`

Revisa los agentes que usa y como funciona el flujo completo.

---

## Output

```json
{
  "success": true
}
```
