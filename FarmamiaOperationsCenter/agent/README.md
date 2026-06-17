# Windows Agent - .NET 8 Worker Service

## Responsabilidad

Servicio Windows instalado en cada equipo POS para recibir instrucciones del servidor central, actualizar el POS portable, respaldar, validar, ejecutar rollback y reportar eventos.

## Ruta POS

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente
```

Ejecutable:

```txt
Zabyca.Pos.Desktop.exe
```

## Estructura local

```txt
C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent
  Farmamia.Agent.Service.exe
  FarmamiaUpdater.exe
  config.json
  Downloads\
  Backups\
  Logs\
  Temp\
  State\
```

## Servicios internos

```txt
ServicioAgente
InicializarAgenteCasoUso
EnviarLatidoCasoUso
PrepararActualizacionCasoUso
InventarioWindows
ClienteOperacionesFarmamia
ConfiguracionLocalAgente
AlmacenamientoPaquetesLocal
EstadoLocalAgenteArchivo
ArchivoLoggerProvider
FarmamiaUpdater
```

## Estado actual

```txt
Base Worker Service creada en Farmamia.Agent.
Estructura interna en espanol.
Nombre de servicio Windows: FarmamiaOpsAgent.
Salida del servicio: Farmamia.Agent.Service.exe.
Ruta final del agente: C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent.
Registro automatico contra POST /api/agent/register.
Persistencia local de credenciales en State\credenciales.json.
Persistencia de ultimo estado local en State\estado-agente.json.
Creacion de config.json y carpetas Downloads, Backups, Logs, Temp y State.
Heartbeat periodico contra POST /api/agent/heartbeat.
Consulta de instrucciones contra GET /api/agent/{deviceId}/instructions.
Reintentos con backoff para inicializacion, ciclos del worker y descarga de paquetes.
El servicio no se detiene si la API central no responde.
Respeto de hora oficial y hora forzada, incluyendo ventanas que cruzan medianoche.
Deteccion de actividad POS y diferimiento hasta hora forzada.
Avisos de usuario en State/Avisos para cada hora de advertencia recibida.
Estado local de avisos enviados para no duplicarlos en cada ciclo.
Descarga de paquete autorizado en Downloads.
Validacion local de SHA-256.
Reporte de eventos POS_ACTIVITY_DETECTED, USER_WARNING_SENT, DOWNLOAD_STARTED, DOWNLOAD_COMPLETED, CHECKSUM_VALIDATED, VALIDATION_FAILED, UPDATE_STARTED, UPDATE_COMPLETED, UPDATE_FAILED, ROLLBACK_STARTED, ROLLBACK_COMPLETED y ROLLBACK_FAILED.
Si el checksum falla, reporta VALIDATION_FAILED y no modifica el POS.
Creacion de respaldo antes de aplicar el paquete.
Cierre del proceso POS antes de modificar archivos.
Aplicacion del ZIP sobre la ruta POS.
Validacion de existencia de Zabyca.Pos.Desktop.exe.
Rollback desde respaldo cuando falla despues de modificar archivos.
Reapertura del POS al completar o restaurar.
Reporte de resultado final COMPLETED, ROLLBACK_COMPLETED o ROLLBACK_FAILED.
Inventario local basico de hostname, IP, MAC, Windows, version POS y disco.
Logs diarios en Logs\agent-yyyyMMdd.log.
Ejecutable manual FarmamiaUpdater con comandos estado, version, buscar, instalar-ahora y diagnostico.
```

## Instalacion como servicio

Publicar los binarios en una carpeta local:

```powershell
.\herramientas\publicar-agente.ps1 `
  -Configuracion Release `
  -Salida .\publicado\Agent `
  -UrlApiCentral http://localhost:8081 `
  -CodigoSucursal FM001
```

Copiar e instalar en la ruta final del agente desde PowerShell como administrador:

```powershell
.\herramientas\publicar-agente.ps1 `
  -Configuracion Release `
  -RutaDestino "C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent" `
  -UrlApiCentral http://servidor-farmamia:8081 `
  -CodigoSucursal FM001 `
  -InstalarServicio
```

Si los binarios ya estan copiados en la ruta final, instalar solo el servicio:

```powershell
.\herramientas\instalar-servicio.ps1
```

El script:

```txt
Crea la estructura local requerida.
Registra el servicio FarmamiaOpsAgent.
Configura inicio automatico.
Configura recuperacion por fallo con reinicio del servicio.
Inicia el servicio.
```

El archivo `config.json` publicado usa esta estructura:

```json
{
  "AgenteFarmamia": {
    "UrlApiCentral": "http://localhost:8081",
    "CodigoSucursal": "FM001",
    "VersionAgente": "1.0.0",
    "RutaPos": "C:\\Program Files (x86)\\Farmamia Cia Ltda - Elipsys\\Cliente",
    "RutaAgente": "C:\\Program Files (x86)\\Farmamia Cia Ltda - Elipsys\\Agent",
    "IntervaloHeartbeatSegundos": 60,
    "TimeoutSegundos": 30,
    "MaxIntentosDescarga": 3,
    "BackoffInicialSegundos": 5,
    "BackoffMaximoSegundos": 300
  }
}
```

Para desinstalar:

```powershell
.\herramientas\desinstalar-servicio.ps1
```

## Ejecutable manual

```powershell
.\FarmamiaUpdater.exe estado
.\FarmamiaUpdater.exe version
.\FarmamiaUpdater.exe buscar
.\FarmamiaUpdater.exe instalar-ahora
.\FarmamiaUpdater.exe diagnostico
```

Uso esperado:

```txt
estado: muestra deviceId, ultimo resultado y ultimo error local.
version: muestra la version POS configurada/local.
buscar: consulta si el servidor tiene una instruccion autorizada.
instalar-ahora: ejecuta el mismo caso de uso del servicio si el servidor autoriza una instruccion.
diagnostico: envia un evento AGENT_DIAGNOSTIC al backend.
```

## Arquitectura

El agente sigue arquitectura limpia y hexagonal:

```txt
Dominio/
  Modelos/
  Puertos/
Aplicacion/
  CasosUso/
Infraestructura/
  Api/
  Actualizacion/
  Almacenamiento/
  Avisos/
  Configuracion/
  Estado/
  Inventario/
  Logging/
  Tiempo/
Servicio/
```

Regla aplicada:

```txt
Los casos de uso no dependen de HttpClient, archivos ni Windows.
Los puertos viven en Dominio.
Los adaptadores tecnicos viven en Infraestructura.
ServicioAgente solo coordina el ciclo de ejecucion del Worker.
```

## Reglas criticas

- No actualizar sin autorizacion del servidor.
- Validar SHA256 antes de aplicar.
- Respaldar antes de modificar archivos.
- Mantener ultimas 3 versiones para rollback.
- Reportar cada evento importante.
- Actualizar al encender si el equipo estuvo apagado durante la ventana.

## Pruebas

```powershell
$env:DOTNET_ROLL_FORWARD='Major'
dotnet test Farmamia.Agent.Tests\Farmamia.Agent.Tests.csproj
```

Cobertura actual:

```txt
ZIP valido.
ZIP sin Zabyca.Pos.Desktop.exe.
ZIP sin Zabyca.Pos.Desktop.exe rechazado antes de copiar archivos al POS.
Checksum incorrecto sin modificar POS.
Backup correcto.
Rollback correcto.
Token persistido despues de reinicio.
Resultado COMPLETED con UPDATE_COMPLETED.
Resultado FAILED con VALIDATION_FAILED.
Reintento de descarga ante API/descarga temporalmente no disponible.
Rollback cuando falla despues de modificar archivos.
```

Nota: en esta maquina se uso `DOTNET_ROLL_FORWARD=Major` porque el runtime instalado disponible es .NET 10, mientras el agente mantiene target `net8.0`.

## Prueba E2E real

La prueba completa en maquina Windows limpia esta documentada en:

```txt
Documentacion/FARMAMIA_OPERATIONS_CENTER_E2E_AGENTE_WINDOWS.md
```

Scripts:

```txt
herramientas/e2e-agente-windows/01-preparar-entorno.ps1
herramientas/e2e-agente-windows/02-instalar-agente.ps1
herramientas/e2e-agente-windows/03-validar-servicio.ps1
herramientas/e2e-agente-windows/04-validar-heartbeat.ps1
herramientas/e2e-agente-windows/05-crear-paquete-demo.ps1
herramientas/e2e-agente-windows/06-crear-despliegue-demo.ps1
herramientas/e2e-agente-windows/07-ejecutar-prueba-exitosa.ps1
herramientas/e2e-agente-windows/08-ejecutar-prueba-fallida.ps1
```
