# API Contract MVP

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

