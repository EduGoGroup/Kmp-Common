---
name: qareview
description: QA review de tasks (ultimo paso del workflow)
---

# QA Review: Validacion Final

Ejecuta QA de tasks. Este es el ULTIMO paso del workflow.
Si aprueba, la task se marca como COMPLETADA.

---

## Configuracion del Proyecto

```
PROJECT_ID: proj-1769654677209332000
PROJECT_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common
DOCS_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/Documents_Analisys
```

---

## Input (acepta cualquiera de estos formatos)

**Task individual**:
```bash
/special:qareview task-configurar-gradle-wrapper-8-11-e58d92f0
```

**Multiples tasks** (separadas por espacio):
```bash
/special:qareview task-xxx task-yyy task-zzz
```

**Story completa** (revisa todas sus tasks):
```bash
/special:qareview story-como-desarrollador-quiero-xxx
```

---

## Que se espera

1. Si llega task_id: buscar informacion de esa task especifica
2. Si llega lista de task_ids: buscar informacion de todas las tasks
3. Si llega story_id: buscar la story y todas las tasks que tiene
4. Debe compilar - si no compila o no pasan los tests, informar al usuario
5. Al terminar, pasar la tarea al siguiente paso si corresponde
6. Crear un work_item con el resultado
7. Si necesitas informacion del backend, consulta DOCS_PATH

---

## Guia de referencia

Adaptate segun el contexto usando como referencia:
`/Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/.claude/commands/061-qa-task.md`

Revisa los agentes que usa y como funciona el flujo completo.

---

## Output

```json
{
  "success": true
}
```
