# Farmamia Operations Center - Prueba E2E Real del Agente Windows

> Objetivo: validar el MVP completo en una maquina Windows limpia simulando una farmacia piloto.

## 1. Alcance

Equipo piloto:

```txt
POS-PILOTO-001
```

Ruta del agente:

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent
```

Ruta POS:

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente
```

La prueba valida:

```txt
Instalacion del agente como servicio Windows.
Registro del agente y persistencia de token tecnico.
Heartbeat contra backend.
Descarga de paquete autorizado.
Validacion SHA256.
Backup local.
Actualizacion POS exitosa.
Fallo controlado con ZIP sin Zabyca.Pos.Desktop.exe.
Rollback cuando aplica.
Eventos, resultado, alertas y visibilidad Angular.
```

## 2. Precondiciones

Backend y panel:

```txt
PostgreSQL levantado.
Backend Spring Boot levantado.
Flyway aplicado.
Usuario ADMIN disponible.
Panel Angular levantado.
```

Maquina piloto:

```txt
Windows limpio.
Sin Visual Studio.
Sin .NET SDK.
Sin herramientas de desarrollo.
Nombre recomendado del equipo: POS-PILOTO-001.
PowerShell disponible.
Permisos de administrador local para instalar servicio.
```

Agente:

```txt
Usar paquete publicado del agente.
Si el paquete es self-contained, no requiere runtime .NET.
Si no es self-contained, requiere .NET Runtime 8 para Windows x64.
```

## 3. Publicar Paquete del Agente

En la maquina de construccion:

```powershell
cd FarmamiaOperationsCenter\windows-agent

.\herramientas\publicar-agente.ps1 `
  -Configuracion Release `
  -Runtime win-x64 `
  -SelfContained `
  -Salida .\publicado\Agent `
  -UrlApiCentral http://SERVIDOR:8081 `
  -CodigoSucursal FMA-PILOTO-001
```

Comprimir la carpeta publicada:

```powershell
Compress-Archive -Path .\publicado\Agent\* -DestinationPath .\publicado\FarmamiaAgent-Piloto.zip -Force
```

Copiar `FarmamiaAgent-Piloto.zip` a la maquina `POS-PILOTO-001`.

## 4. Preparar Maquina Piloto

Ejecutar PowerShell como administrador:

```powershell
cd C:\Ruta\FarmamiaOperationsCenter\herramientas\e2e-agente-windows

.\01-preparar-entorno.ps1 `
  -NombreEquipoEsperado POS-PILOTO-001
```

Validar:

```txt
Ruta POS creada.
Zabyca.Pos.Desktop.exe base existe.
version.txt base existe.
```

## 5. Instalar Agente

```powershell
.\02-instalar-agente.ps1 `
  -PaqueteAgente C:\Instaladores\FarmamiaAgent-Piloto.zip `
  -ApiBaseUrl http://SERVIDOR:8081 `
  -CodigoSucursal FMA-PILOTO-001
```

Validar servicio:

```powershell
.\03-validar-servicio.ps1
```

Debe confirmar:

```txt
FarmamiaOpsAgent instalado.
StartType Automatic.
Farmamia.Agent.Service.exe existe.
FarmamiaUpdater.exe existe.
config.json existe.
Downloads, Backups, Logs, Temp y State existen.
```

## 6. Validar Registro y Heartbeat

```powershell
.\04-validar-heartbeat.ps1 `
  -ApiBaseUrl http://SERVIDOR:8081 `
  -UsuarioAdmin admin `
  -ContrasenaAdmin admin123
```

Debe confirmar:

```txt
State\credenciales.json existe.
deviceId existe.
token tecnico existe.
El equipo existe en backend.
Heartbeat recibido.
```

## 7. Prueba E2E Exitosa

```powershell
.\07-ejecutar-prueba-exitosa.ps1 `
  -ApiBaseUrl http://SERVIDOR:8081 `
  -Version 2026.06.3-piloto `
  -UsuarioAdmin admin `
  -ContrasenaAdmin admin123
```

El script ejecuta:

```txt
Crea ZIP valido con Zabyca.Pos.Desktop.exe.
Carga paquete.
Aprueba paquete.
Crea despliegue contra el deviceId local.
Ejecuta FarmamiaUpdater.exe buscar.
Ejecuta FarmamiaUpdater.exe instalar-ahora.
Valida estado COMPLETED.
Valida evento UPDATE_COMPLETED.
Valida posVersion actualizada.
Valida ausencia de alerta critica abierta.
Guarda resultados/resultados-e2e-exitoso.json.
```

Validaciones manuales:

```txt
Downloads contiene ZIP descargado.
Backups contiene respaldo.
Logs contiene agent-yyyyMMdd.log.
State\estado-agente.json indica UPDATE_COMPLETED.
En Angular, equipo actualizado.
En Angular, despliegue completado.
En Angular, evento UPDATE_COMPLETED visible.
En Angular, sin alerta critica asociada.
```

## 8. Prueba E2E Fallida

Modo soportado para prueba real reproducible:

```txt
SinEjecutable: ZIP sin Zabyca.Pos.Desktop.exe.
```

Ejecutar:

```powershell
.\08-ejecutar-prueba-fallida.ps1 `
  -ApiBaseUrl http://SERVIDOR:8081 `
  -Version 2026.06.3-piloto-fail `
  -Modo SinEjecutable `
  -UsuarioAdmin admin `
  -ContrasenaAdmin admin123
```

El script valida:

```txt
Target queda FAILED.
posVersion no cambia.
Existe VALIDATION_FAILED o UPDATE_FAILED.
Existe alerta critica abierta.
State\estado-agente.json registra fallo/rollback segun corresponda.
Guarda resultados/resultado-e2e-fallido.json.
```

Validaciones manuales:

```txt
En Angular, target FAILED.
En Angular, evento de fallo visible.
En Angular, alerta critica visible.
La version POS del equipo no cambia.
Logs locales muestran mensaje tecnico.
```

## 9. Checklist E2E

| Item | Esperado | Resultado |
|---|---|---|
| Backend inicia | API disponible | Pendiente |
| PostgreSQL inicia | Conexion correcta | Pendiente |
| Flyway aplica | Migraciones OK | Pendiente |
| Angular inicia | Panel accesible | Pendiente |
| Agente instalado | Servicio FarmamiaOpsAgent | Pendiente |
| Arranque automatico | StartType Automatic | Pendiente |
| Carpetas locales | Downloads/Backups/Logs/Temp/State | Pendiente |
| Registro | deviceId y token tecnico | Pendiente |
| Heartbeat | Equipo online/actualizado | Pendiente |
| Paquete valido | APPROVED | Pendiente |
| Despliegue exitoso | RUNNING/COMPLETED | Pendiente |
| Updater buscar | Instruccion UPDATE_POS | Pendiente |
| Updater instalar-ahora | COMPLETED | Pendiente |
| Backup | Carpeta creada | Pendiente |
| Evento exitoso | UPDATE_COMPLETED | Pendiente |
| Version POS | 2026.06.3-piloto | Pendiente |
| Alertas exito | Sin alerta critica | Pendiente |
| Paquete fallido | APPROVED | Pendiente |
| Despliegue fallido | FAILED | Pendiente |
| Evento fallido | VALIDATION_FAILED/UPDATE_FAILED | Pendiente |
| Version POS fallida | No cambia | Pendiente |
| Alertas fallo | Critica visible | Pendiente |
| Angular | Equipo/despliegue/eventos/alertas visibles | Pendiente |

## 10. Documento de Resultados

Guardar evidencias en:

```txt
herramientas\e2e-agente-windows\resultados
```

Archivos esperados:

```txt
resultado-e2e-exitoso.json
resultado-e2e-fallido.json
capturas-angular-dashboard.png
capturas-angular-alertas.png
capturas-angular-despliegue.png
agent-log.txt
```

## 11. Riesgos Encontrados

| Riesgo | Impacto | Mitigacion |
|---|---:|---|
| Runtime .NET faltante si el agente no es self-contained | Alto | Publicar con `-SelfContained` para pilotos |
| Hostname distinto a POS-PILOTO-001 | Medio | Renombrar equipo o aceptar hostname real en backend |
| Permisos insuficientes en Program Files | Alto | Instalar con PowerShell administrador |
| API no disponible durante registro | Medio | Backoff del agente y validacion con logs |
| ZIP sin ejecutable podia pasar si quedaba ejecutable anterior | Alto | Corregido: el agente valida el ZIP extraido antes de copiar |
| Alertas no visibles por filtros Angular | Medio | Validar Dashboard y pantalla Alertas con filtros limpios |
| Credenciales admin demo en ambiente real | Alto | Cambiar credenciales y secretos antes de piloto real |

## 12. Recomendaciones Antes de Fase 2

```txt
Ejecutar esta prueba en al menos 2 equipos Windows limpios.
Usar paquete self-contained para evitar dependencia de runtime.
Crear checklist firmado por soporte/operaciones.
Definir sucursales piloto y grupos TRX.
Definir ventana horaria real.
Validar ancho de banda con paquete de tamano similar al POS real.
Activar monitoreo basico de logs y alertas.
Cambiar secretos y credenciales demo.
Probar recuperacion despues de reiniciar Windows.
Probar API caida temporal durante heartbeat y descarga.
```

Decision:

```txt
Si prueba exitosa y fallida pasan, el MVP queda apto para iniciar Fase 2: Pilotos, Grupos TRX y Farmacias de Turno.
```
