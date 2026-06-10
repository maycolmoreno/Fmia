export interface EstadoSaludApi {
  service?: string;
  serverTime?: string;
  status: string;
}

export interface RespuestaLogin {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  username: string;
  fullName: string;
  role: string;
}

export type RolUsuarioAdministrativo = 'ADMIN' | 'OPERATOR' | 'AUDITOR' | 'VIEWER';

export interface UsuarioAdministrativo {
  id: string;
  username: string;
  fullName: string;
  email?: string | null;
  role: RolUsuarioAdministrativo;
  active: boolean;
  failedLoginAttempts: number;
  lockedUntil?: string | null;
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaquetePos {
  id: string;
  version: string;
  fileName: string;
  sha256Checksum: string;
  sizeBytes: number;
  status: string;
  downloadUrl: string;
  uploadedAt: string;
  approvedAt?: string | null;
}

export interface Sucursal {
  id: string;
  code: string;
  name: string;
  city?: string | null;
  zone?: string | null;
  address?: string | null;
  onDuty: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Equipo {
  id: string;
  branchId: string;
  branchCode: string;
  branchName: string;
  hostname: string;
  ipAddress?: string | null;
  macAddress?: string | null;
  windowsVersion?: string | null;
  agentVersion?: string | null;
  posVersion?: string | null;
  posPath: string;
  status: string;
  lastHeartbeatAt?: string | null;
  registeredAt: string;
  updatedAt: string;
}

export interface MetricaEquipo {
  id: string;
  posVersion?: string | null;
  diskFreeMb?: number | null;
  diskTotalMb?: number | null;
  posProcessRunning?: boolean | null;
  latencyMs?: number | null;
  packetLossPercent?: number | null;
  agentStatus?: string | null;
  collectedAt: string;
}

export interface ObjetivoEquipo {
  targetId: string;
  deploymentId: string;
  deploymentName: string;
  packageVersion: string;
  deploymentStatus: string;
  targetStatus: string;
  targetGroup?: string | null;
  pilot: boolean;
  oldVersion?: string | null;
  newVersion?: string | null;
  lastError?: string | null;
  authorizedAt?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  updatedAt: string;
}

export interface DetalleEquipo {
  device: Equipo;
  lastMetric?: MetricaEquipo | null;
  recentEvents: EventoActualizacion[];
  deployments: ObjetivoEquipo[];
}

export interface Despliegue {
  id: string;
  packageId: string;
  packageVersion: string;
  name: string;
  description?: string | null;
  status: string;
  scheduledAt?: string | null;
  createdAt: string;
  targetCount: number;
}

export interface EventoActualizacion {
  id: string;
  deviceId: string;
  hostname: string;
  deploymentId?: string | null;
  deploymentTargetId?: string | null;
  eventType: string;
  eventMessage?: string | null;
  oldVersion?: string | null;
  newVersion?: string | null;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export interface AlertaOperativa {
  id: string;
  deviceId: string;
  hostname: string;
  branchId: string;
  branchCode: string;
  severity: string;
  alertType: string;
  title: string;
  message?: string | null;
  status: string;
  openedAt: string;
  acknowledgedBy?: string | null;
  acknowledgedAt?: string | null;
  closedBy?: string | null;
  closedAt?: string | null;
}

export interface AuditoriaAdministrativa {
  id: string;
  actorUserId?: string | null;
  actorUsername?: string | null;
  action: string;
  entityType: string;
  entityId?: string | null;
  oldValues: Record<string, unknown>;
  newValues: Record<string, unknown>;
  ipAddress?: string | null;
  createdAt: string;
}

export interface EstadoDespliegue {
  deploymentId: string;
  status: string;
  targetsByStatus: Record<string, number>;
}

export interface SolicitudCrearDespliegue {
  packageId: string;
  name: string;
  description?: string | null;
  scheduledAt?: string | null;
  targetGroup?: string | null;
  pilot: boolean;
  deviceIds: string[];
}
