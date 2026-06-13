# P0-001 - Grupo TRX Formal

## Objetivo

Formalizar Grupo TRX como entidad operacional de Farmamia Operations Center para controlar oleadas de actualizacion POS, limitar impacto, medir fallos y permitir pausas controladas.

Grupo TRX no es una etiqueta generica ni un reemplazo universal de segmentacion. Existe para proteger farmacias y equipos POS durante campanas POS.

## Alcance implementado

- Entidad operacional `GrupoTrx`.
- Estados `ACTIVO`, `PAUSADO`, `RETIRADO`.
- Limite maximo de 100 equipos POS por grupo.
- Asignacion unica de Equipo POS a Grupo TRX.
- Relacion de grupo con equipos de multiples farmacias.
- API `/api/grupos-trx`.
- Vista Angular `Grupos TRX`.
- Auditoria de acciones operativas.
- Migracion Flyway sin eliminar `targetGroup` legacy.

## Reglas activas

| Regla | Estado |
| ----- | ------ |
| Maximo 100 equipos POS por grupo | Implementada |
| Un Equipo POS pertenece como maximo a un Grupo TRX activo | Implementada |
| Un Grupo TRX puede contener equipos de varias farmacias | Implementada |
| Grupo `PAUSADO` no acepta nuevas asignaciones | Implementada |
| Grupo `RETIRADO` no acepta nuevas asignaciones | Implementada |
| Grupo usado historicamente no se elimina fisicamente | Implementada como retiro operacional |
| Auditoria obligatoria | Implementada |

## API

```http
GET    /api/grupos-trx
GET    /api/grupos-trx/page
GET    /api/grupos-trx/{id}
POST   /api/grupos-trx
PUT    /api/grupos-trx/{id}
POST   /api/grupos-trx/{id}/pausar
POST   /api/grupos-trx/{id}/reanudar
POST   /api/grupos-trx/{id}/retirar
POST   /api/grupos-trx/{id}/equipos/{equipoId}
DELETE /api/grupos-trx/{id}/equipos/{equipoId}
```

Filtros paginados:

- `codigo`
- `estado`
- `activo`
- `page`
- `size`
- `sort`

## Auditoria

Acciones registradas:

- `CREAR_GRUPO_TRX`
- `MODIFICAR_GRUPO_TRX`
- `PAUSAR_GRUPO_TRX`
- `REANUDAR_GRUPO_TRX`
- `RETIRAR_GRUPO_TRX`
- `ASIGNAR_EQUIPO_TRX`
- `QUITAR_EQUIPO_TRX`

## Compatibilidad

`targetGroup` en campanas/oleadas se mantiene por compatibilidad. Esta historia introduce el modelo formal para que futuras campanas POS puedan migrar gradualmente hacia `GrupoTrx`.

## Fuera de alcance

- Balance automatico.
- Simulacion.
- Motor inteligente de segmentacion.
- Farmacias de turno especiales.
- Pausa automatica.
- IA.
- Dashboard avanzado.
