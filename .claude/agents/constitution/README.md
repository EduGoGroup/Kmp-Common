# Constitution Agents

Agentes troceados para crear proyectos. Orquestados por `/011-constitution-create-project`.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│           011-constitution-create-project.md                     │
│                    (ORQUESTADOR)                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  common/        │  │  constitution/   │  │  MCP Directo    │
│  mcp-validator  │  │  analyzer        │  │  (tracking)     │
│  search-local   │  │  project-creator │  │                 │
│  search-internet│  │  document-finder │  │                 │
│  document-loader│  │  doc-associator  │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Agentes

| Agente | Responsabilidad |
|--------|-----------------|
| `analyzer-agent.md` | Analiza descripción, infiere tech/kind/level |
| `project-creator-agent.md` | Crea proyecto en BD |
| `document-finder-agent.md` | Determina docs necesarios |
| `document-associator-agent.md` | Asocia docs al proyecto |

## Principios

1. **Single Responsibility**: Cada agente hace UNA cosa
2. **No Task() interno**: El comando orquesta
3. **Datos explícitos**: Orquestador pasa datos entre agentes

**Versión**: 1.0
