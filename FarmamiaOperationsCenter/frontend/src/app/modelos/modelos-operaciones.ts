export interface EstadoSaludApi {
  service?: string;
  serverTime?: string;
  status: string;
}

export interface RespuestaPagina<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
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

export interface VersionPos {
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

export interface Farmacia {
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

export interface EstadoOperacionalFarmacia {
  farmaciaId: string;
  codigoFarmacia: string;
  nombreFarmacia: string;
  ciudad?: string | null;
  zona?: string | null;
  deTurno: boolean;
  activa: boolean;
  estadoOperacional: string;
  critica: boolean;
  turnoEnRiesgo: boolean;
  totalEquiposPos: number;
  equiposOnline: number;
  equiposOffline: number;
  equiposSinLatido: number;
  ultimoLatidoEn?: string | null;
  alertasAbiertas: number;
  alertasCriticas: number;
  campanasActivas: number;
  objetivosCampanaPendientes: number;
  objetivosCampanaFallidos: number;
  campanaActivaPrincipal?: string | null;
  grupoTrxPrincipal?: string | null;
  versionPosDominante?: string | null;
  resumenRiesgo: string;
}

export interface EquipoGrupoTrx {
  deviceId: string;
  hostname: string;
  branchId: string;
  branchCode: string;
  branchName: string;
  posVersion?: string | null;
  deviceStatus: string;
  lastHeartbeatAt?: string | null;
  assignedAt: string;
}

export interface GrupoTrx {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: 'ACTIVO' | 'PAUSADO' | 'RETIRADO';
  maxDevices: number;
  active: boolean;
  assignedDevices: number;
  involvedBranches: number;
  createdAt: string;
  updatedAt: string;
  devices: EquipoGrupoTrx[];
  branchCodes: string[];
}

export interface EquipoPos {
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

export type EstadoSugerenciaHuerfano = 'SUGERENCIA_VALIDA' | 'FARMACIA_NO_EXISTE' | 'FORMATO_INVALIDO';

export interface EquipoHuerfano {
  deviceId: string;
  hostname: string;
  ipAddress?: string | null;
  agentVersion?: string | null;
  posVersion?: string | null;
  registeredAt: string;
  suggestionStatus: EstadoSugerenciaHuerfano;
  suggestedBranchId?: string | null;
  suggestedBranchCode?: string | null;
  suggestedBranchName?: string | null;
  suggestedGrupoTrxCode?: string | null;
}

export interface ResumenAsignacionMasiva {
  assigned: number;
  skipped: number;
}

export interface MetricaEquipoPos {
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

export interface ObjetivoCampanaEquipoPos {
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

export interface DetalleEquipoPos {
  device: EquipoPos;
  lastMetric?: MetricaEquipoPos | null;
  recentEvents: EventoAgente[];
  deployments: ObjetivoCampanaEquipoPos[];
}

export interface CampanaPos {
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

export interface EquipoEstadoCampanaFarmacia {
  deviceId: string;
  hostname: string;
  deviceStatus: string;
  targetStatus: string;
  grupoTrx?: string | null;
  oldVersion?: string | null;
  newVersion?: string | null;
  lastError?: string | null;
  lastHeartbeatAt?: string | null;
  rollback: boolean;
}

export interface EstadoCampanaFarmacia {
  farmaciaId: string;
  codigoFarmacia: string;
  nombreFarmacia: string;
  campanaId: string;
  grupoTrxId?: string | null;
  codigoGrupoTrx?: string | null;
  deTurno: boolean;
  totalEquiposPos: number;
  completados: number;
  pendientes: number;
  fallidos: number;
  rollbacks: number;
  ultimoHeartbeatRelacionado?: string | null;
  alertasCriticas: number;
  alertasAbiertas: number;
  estadoTecnico: string;
  estadoOperacional: string;
  resumenRiesgo: string;
  devices: EquipoEstadoCampanaFarmacia[];
}

export interface ResumenEstadoCampanaFarmacia {
  campanaId: string;
  nombreCampana: string;
  versionPos: string;
  estadoCampana: string;
  totalFarmacias: number;
  farmaciasCompletadas: number;
  farmaciasPendientes: number;
  farmaciasEnProgreso: number;
  farmaciasConErrores: number;
  farmaciasEnRiesgo: number;
  farmaciasCriticas: number;
  farmaciasTurnoEnRiesgo: number;
  avancePorcentaje: number;
  exitoPorcentaje: number;
  grupoTrxPeorEstado?: string | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  farmacias: EstadoCampanaFarmacia[];
}

export interface CampanaGrupoTrx {
  id?: string | null;
  campanaId: string;
  nombreCampana: string;
  versionPos: string;
  estadoCampana: string;
  grupoTrxId: string;
  codigoGrupoTrx: string;
  nombreGrupoTrx: string;
  orden: number;
  estado: string;
  totalFarmacias: number;
  farmaciasAfectadas: number;
  farmaciasTurnoAfectadas: number;
  farmaciasCriticas: number;
  farmaciasPendientes: number;
  farmaciasConFallos: number;
  equiposPosTotales: number;
  equiposPosCompletados: number;
  equiposPosPendientes: number;
  equiposPosFallidos: number;
  rollbacks: number;
  motivoPausa?: string | null;
  resumenRiesgo: string;
  iniciadoEn?: string | null;
  finalizadoEn?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  farmacias: EstadoCampanaFarmacia[];
}

export interface ResumenCampanaGruposTrx {
  campanaId: string;
  nombreCampana: string;
  versionPos: string;
  estadoCampana: string;
  totalGrupos: number;
  gruposEnRiesgo: number;
  gruposPausados: number;
  farmaciasAfectadas: number;
  farmaciasTurnoAfectadas: number;
  farmaciasCriticas: number;
  grupos: CampanaGrupoTrx[];
}

export interface EventoAgente {
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
  deviceId?: string | null;
  hostname?: string | null;
  branchId?: string | null;
  branchCode: string;
  severity: string;
  alertType: string;
  title: string;
  message?: string | null;
  status: string;
  openedAt: string;
  networkEvent?: boolean | null;
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
  totalTargets: number;
  completedTargets: number;
  failedTargets: number;
  pendingTargets: number;
  progressPercent: number;
  failurePercent: number;
  targetsByStatus: Record<string, number>;
}

export interface OleadaOrquestacion {
  id: string;
  number: number;
  name: string;
  targetGroup?: string | null;
  pilot: boolean;
  status: string;
  plannedTargets: number;
  completedTargets: number;
  failedTargets: number;
  pendingTargets: number;
  onDutyBranches: number;
  failurePercent: number;
  maintenanceWindowStart?: string | null;
  maintenanceWindowEnd?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
}

export interface PlanOrquestacion {
  deploymentId: string;
  controlStatus: string;
  maxFailurePercent: number;
  autoPauseEnabled: boolean;
  retryLimit: number;
  nextWaveNumber: number;
  pausedReason?: string | null;
  lastEvaluatedAt?: string | null;
  waves: OleadaOrquestacion[];
}

export interface SolicitudPlanOrquestacion {
  maxFailurePercent: number;
  autoPauseEnabled: boolean;
  retryLimit: number;
  maxParallelDevices: number;
  maintenanceWindowStart?: string | null;
  maintenanceWindowEnd?: string | null;
}

export interface SolicitudCrearDespliegue {
  packageId: string;
  name: string;
  description?: string | null;
  scheduledAt?: string | null;
  targetGroup?: string | null;
  pilot: boolean;
  deviceIds?: string[];
}

// --- NOC Dashboard ---

export interface FarmaciaCriticaNoc {
  id: string;
  code: string;
  name: string;
  onDuty: boolean;
  operationalStatus: string;
  critical: boolean;
  dutyAtRisk: boolean;
  criticalAlerts: number;
  riskSummary: string;
}

export interface EstadoRedNoc {
  linkDown: number;
  highLatency: number;
  vpnDown: number;
}

export interface EstadoPosNoc {
  total: number;
  online: number;
  offline: number;
  atRisk: number;
  currentVersion: string | null;
}

export interface CampanaActivaNoc {
  id: string;
  name: string;
  posVersion: string | null;
  progressPercent: number;
  totalDevices: number;
  completed: number;
  failed: number;
}

export interface AlertaResumenNoc {
  id: string;
  farmId: string | null;
  farmCode: string | null;
  severity: string;
  alertType: string;
  title: string;
  status: string;
  openedAt: string;
  networkEvent: boolean;
}

export interface ResumenNocDashboard {
  criticFarms: FarmaciaCriticaNoc[];
  atRiskFarms: FarmaciaCriticaNoc[];
  network: EstadoRedNoc;
  pos: EstadoPosNoc;
  activeCampaign: CampanaActivaNoc | null;
  recentAlerts: AlertaResumenNoc[];
  generatedAt: string;
}

// --- type aliases (backwards compat) ---

export type PaquetePos = VersionPos;
export type Sucursal = Farmacia;
export type Equipo = EquipoPos;
export type MetricaEquipo = MetricaEquipoPos;
export type ObjetivoEquipo = ObjetivoCampanaEquipoPos;
export type DetalleEquipo = DetalleEquipoPos;
export type Despliegue = CampanaPos;
export type EventoActualizacion = EventoAgente;
