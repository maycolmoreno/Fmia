# API Contract MVP

## Lenguaje canonico y compatibilidad

El lenguaje publico del Operations Center esta orientado al negocio Farmamia. Los endpoints nuevos deben preferir:

```http
GET /api/farmacias
GET /api/equipos-pos
GET /api/versiones-pos
GET /api/campanas-pos
GET /api/eventos-agente
```

Los endpoints legacy se mantienen por compatibilidad temporal:

```http
GET /api/branches
GET /api/devices
GET /api/packages
GET /api/deployments
GET /api/update-events
```

Ambos grupos delegan a los mismos casos de uso y devuelven el mismo payload en la etapa 1 de reorientacion de dominio. El agente Windows no se migra todavia y conserva sus rutas actuales bajo `/api/agent`.

### Estado operacional de farmacias

```http
GET /api/farmacias/estado
GET /api/farmacias/{id}/estado
```

Respuesta:

```json
{
  "farmaciaId": "uuid",
  "codigoFarmacia": "FM001",
  "nombreFarmacia": "Farmacia Centro",
  "deTurno": true,
  "estadoOperacional": "CRITICA",
  "critica": true,
  "turnoEnRiesgo": true,
  "totalEquiposPos": 3,
  "equiposOnline": 2,
  "equiposOffline": 1,
  "alertasAbiertas": 1,
  "alertasCriticas": 1,
  "campanasActivas": 1,
  "objetivosCampanaPendientes": 1,
  "objetivosCampanaFallidos": 0,
  "campanaActivaPrincipal": "Campana POS 2026.08.01",
  "grupoTrxPrincipal": "trx001",
  "versionPosDominante": "2026.06",
  "resumenRiesgo": "Farmacia de turno con riesgo operativo"
}
```

Reglas iniciales para farmacias de turno:

- `POS_OFFLINE`, heartbeat vencido, `UPDATE_FAILED`, `ROLLBACK_FAILED` o campana pendiente fuera de ventana elevan la farmacia a `CRITICA`.
- `ROLLBACK_COMPLETED` o Grupo TRX pausado con campaña pendiente elevan la farmacia a `EN_RIESGO`.
- La creación de una campaña que incluya farmacias de turno registra auditoría `ADVERTENCIA_CAMPANA_CON_TURNO`; no bloquea todavía la operación.

### Estado de campana por farmacia

Lectura operacional para que el NOC vea una campana POS por impacto en farmacias.

```http
GET /api/campanas-pos/{id}/estado-por-farmacia
```

Filtros opcionales:

- `estadoTecnico`
- `estadoOperacional`
- `grupoTrx`
- `deTurno`
- `q`
- `page`
- `size`
- `sort`

Respuesta:

```json
{
  "campanaId": "uuid",
  "nombreCampana": "Campana POS 2026.08.01",
  "versionPos": "2026.08.01",
  "estadoCampana": "RUNNING",
  "totalFarmacias": 180,
  "farmaciasCompletadas": 92,
  "farmaciasPendientes": 61,
  "farmaciasEnProgreso": 19,
  "farmaciasConErrores": 8,
  "farmaciasEnRiesgo": 5,
  "farmaciasCriticas": 2,
  "farmaciasTurnoEnRiesgo": 3,
  "avancePorcentaje": 62.5,
  "exitoPorcentaje": 94.0,
  "grupoTrxPeorEstado": "trx001",
  "page": 0,
  "size": 20,
  "totalElements": 180,
  "totalPages": 9,
  "hasNext": true,
  "farmacias": [
    {
      "farmaciaId": "uuid",
      "codigoFarmacia": "FM001",
      "nombreFarmacia": "Farmacia Centro",
      "campanaId": "uuid",
      "grupoTrxId": "uuid",
      "codigoGrupoTrx": "trx001",
      "deTurno": true,
      "totalEquiposPos": 3,
      "completados": 2,
      "pendientes": 1,
      "fallidos": 0,
      "rollbacks": 0,
      "ultimoHeartbeatRelacionado": "2026-06-12T23:59:00Z",
      "alertasCriticas": 1,
      "alertasAbiertas": 1,
      "estadoTecnico": "EN_PROGRESO",
      "estadoOperacional": "CRITICA",
      "resumenRiesgo": "Alerta critica abierta durante campana POS",
      "devices": []
    }
  ]
}
```

### Grupos TRX

### Estado de campana por Grupo TRX

Lectura operacional Farmacia First. Grupo TRX es mecanismo de control; las metricas principales son farmacias.

```http
GET    /api/campanas-pos/{id}/estado-por-trx
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}
DELETE /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}/pausar
POST   /api/campanas-pos/{id}/grupos-trx/{grupoTrxId}/reanudar
```

Respuesta:

```json
{
  "campanaId": "uuid",
  "nombreCampana": "Campana POS 2026.08.01",
  "versionPos": "2026.08.01",
  "estadoCampana": "RUNNING",
  "totalGrupos": 1,
  "gruposEnRiesgo": 1,
  "gruposPausados": 0,
  "farmaciasAfectadas": 8,
  "farmaciasTurnoAfectadas": 2,
  "farmaciasCriticas": 3,
  "grupos": [
    {
      "grupoTrxId": "uuid",
      "codigoGrupoTrx": "trx001",
      "estado": "EN_RIESGO",
      "totalFarmacias": 42,
      "farmaciasAfectadas": 8,
      "farmaciasTurnoAfectadas": 2,
      "farmaciasCriticas": 3,
      "farmaciasPendientes": 12,
      "farmaciasConFallos": 4,
      "equiposPosTotales": 126,
      "equiposPosCompletados": 95,
      "equiposPosPendientes": 20,
      "equiposPosFallidos": 11,
      "rollbacks": 2,
      "resumenRiesgo": "3 farmacias criticas y 2 farmacias de turno afectadas",
      "farmacias": []
    }
  ]
}
```

Reglas:

- `grupoTrxId` es fuente principal.
- `targetGroup` queda como fallback legacy cuando el objetivo no tiene Grupo TRX formal.
- Pausar un Grupo TRX dentro de campana bloquea nuevas instrucciones del agente para objetivos pendientes de ese grupo.
- Los KPIs TRX siempre deben incluir `totalFarmacias`, `farmaciasAfectadas`, `farmaciasTurnoAfectadas` y `farmaciasCriticas`.

Grupo TRX es el mecanismo operacional formal para controlar oleadas POS. El campo legacy `targetGroup` se conserva solo como compatibilidad/fallback.

```http
GET    /api/grupos-trx
GET    /api/grupos-trx/page?codigo=trx&estado=ACTIVO&activo=true
GET    /api/grupos-trx/{id}
POST   /api/grupos-trx
PUT    /api/grupos-trx/{id}
POST   /api/grupos-trx/{id}/pausar
POST   /api/grupos-trx/{id}/reanudar
POST   /api/grupos-trx/{id}/retirar
POST   /api/grupos-trx/{id}/equipos/{equipoId}
DELETE /api/grupos-trx/{id}/equipos/{equipoId}
```

Request crear/actualizar:

```json
{
  "codigo": "trx001",
  "nombre": "TRX 001",
  "descripcion": "Oleada controlada POS",
  "maximoEquipos": 100,
  "activo": true
}
```

Respuesta:

```json
{
  "id": "uuid",
  "code": "trx001",
  "name": "TRX 001",
  "description": "Oleada controlada POS",
  "status": "ACTIVO",
  "maxDevices": 100,
  "active": true,
  "assignedDevices": 82,
  "involvedBranches": 41,
  "createdAt": "2026-06-12T10:00:00Z",
  "updatedAt": "2026-06-12T10:00:00Z",
  "devices": [],
  "branchCodes": []
}
```

## Seguridad

El agente se autentica con token tecnico por equipo.

Encabezado:

```http
Authorization: Bearer {agent_token}
```

El panel administrativo usa JWT de usuario.

---

## Agent API

### Registrar equipo

```http
POST /api/agent/register
```

Request:

```json
{
  "branchCode": "FM001",
  "hostname": "POS-FM001-01",
  "ipAddress": "10.10.1.21",
  "macAddress": "00-11-22-33-44-55",
  "windowsVersion": "Windows 10 Pro",
  "agentVersion": "1.0.0",
  "posVersion": "2026.01.01",
  "posPath": "C:\\Program Files (x86)\\Farmamia Cia Ltda - Elipsys\\Cliente"
}
```

Response:

```json
{
  "deviceId": "uuid",
  "agentToken": "token",
  "serverTime": "2026-06-07T23:55:00"
}
```

### Heartbeat

```http
POST /api/agent/heartbeat
```

Request:

```json
{
  "deviceId": "uuid",
  "posVersion": "2026.01.01",
  "diskFreeMb": 120000,
  "diskTotalMb": 250000,
  "posProcessRunning": true,
  "latencyMs": 42,
  "packetLossPercent": 0.0
}
```

### Consultar instrucciones

```http
GET /api/agent/{deviceId}/instructions
```

Response sin instruccion:

```json
{
  "hasInstruction": false
}
```

Response con actualizacion:

```json
{
  "hasInstruction": true,
  "instructionType": "UPDATE_POS",
  "deploymentTargetId": "uuid",
  "packageId": "uuid",
  "version": "2026.02.01",
  "downloadUrl": "/api/packages/uuid/download",
  "sha256Checksum": "64-char-sha256",
  "officialUpdateTime": "23:55:00",
  "forceUpdateTime": "01:00:00",
  "warnings": [
    "00:50:00",
    "00:55:00"
  ]
}
```

### Registrar evento

```http
POST /api/agent/{deviceId}/events
```

Request:

```json
{
  "deploymentTargetId": "uuid",
  "eventType": "BACKUP_CREATED",
  "eventMessage": "Backup creado correctamente",
  "oldVersion": "2026.01.01",
  "newVersion": "2026.02.01",
  "metadata": {
    "backupPath": "C:\\ProgramData\\FarmamiaOps\\Backups\\2026.01.01"
  }
}
```

### Resultado de actualizacion

```http
POST /api/agent/{deviceId}/update-result
```

Request exitoso:

```json
{
  "deploymentTargetId": "uuid",
  "status": "COMPLETED",
  "oldVersion": "2026.01.01",
  "newVersion": "2026.02.01",
  "message": "Actualizacion completada"
}
```

Request fallido con rollback:

```json
{
  "deploymentTargetId": "uuid",
  "status": "ROLLBACK_COMPLETED",
  "oldVersion": "2026.01.01",
  "newVersion": "2026.02.01",
  "message": "Fallo validacion, rollback completado"
}
```

---

## Package API

```http
POST /api/packages
GET  /api/packages
GET  /api/packages/{id}
POST /api/packages/{id}/approve
POST /api/packages/{id}/retire
GET  /api/packages/{id}/download
```

---

## Deployment API

```http
POST /api/deployments
GET  /api/deployments
GET  /api/deployments/{id}
POST /api/deployments/{id}/schedule
POST /api/deployments/{id}/pause
POST /api/deployments/{id}/resume
POST /api/deployments/{id}/cancel
GET  /api/deployments/{id}/status
```
