# Admin Panel - Angular

## Responsabilidad

Panel web de operaciones para gestionar farmacias, equipos POS, versiones POS, campanas POS, pilotos, grupos TRX, alertas operativas, auditoria y dashboards NOC.

## Modulos iniciales

```txt
auth
dashboard-noc
farmacias
equipos-pos
versiones-pos
campanas-pos
alertas-operativas
audit
```

## Pantallas MVP

```txt
Login
Dashboard NOC
Listado de farmacias
Listado de equipos POS
Detalle operativo de equipo POS
Carga de artefacto de version POS
Listado de versiones POS
Crear campana POS
Estado de campana POS
Eventos del agente
Alertas operativas
Auditoria
```

## Stack recomendado

```txt
Angular 17+
Angular Material
RxJS
JWT Interceptor
Route Guards
Charts
Lazy Loading
```

## Estado actual

Base Angular inicial creada para el MVP con estructura en espanol:

```txt
src/app
  modelos/
  rutas/
  servicios/
```

Pantallas implementadas:

```txt
Login administrativo
Route guard administrativo para proteger /operaciones
Dashboard NOC
Listado de farmacias
Listado de equipos POS inventariados
Detalle operativo de equipo POS
Listado y carga de versiones POS
Aprobacion, retiro y descarga de versiones POS
Listado y creacion de campanas POS
Consulta de estado de campana POS
Seleccion de equipos POS registrados para crear campanas POS
Listado de eventos recientes del agente
Listado de alertas operativas recientes
Listado de auditoria administrativa reciente
```

La API base de desarrollo esta en:

```txt
http://localhost:8081
```

La URL vive en:

```txt
src/environments/environment.ts
```

En build production se reemplaza por:

```txt
src/environments/environment.prod.ts
```

Alli `apiBaseUrl` queda vacio para usar rutas relativas desde el mismo origen del panel/reverse proxy.

Credenciales locales de desarrollo:

```txt
Usuario: admin
Contrasena: admin123
```

El panel guarda el JWT en `localStorage` y lo envia a la API mediante interceptor HTTP.
La ruta `/operaciones` esta protegida por `guardiaAdmin`; si no hay JWT vigente, redirige a `/login`.
La vista Seguridad permite cambiar la contrasena del usuario autenticado. La nueva contrasena debe tener al menos 10 caracteres e incluir mayusculas, minusculas y numeros.

Comandos:

```bash
npm install
npm start
```

Para poblar datos demo en el panel, levantar primero la API y ejecutar desde la raiz de `FarmamiaOperationsCenter`:

```powershell
powershell -ExecutionPolicy Bypass -File .\herramientas\desarrollo\registrar-agente-demo.ps1
```

Para poblar tambien paquetes y despliegues:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-despliegue-demo.ps1
```

Para generar una alerta critica visible en Dashboard y Alertas:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-alerta-fallo-demo.ps1
```

Para generar una actualizacion exitosa visible en Dashboard, Equipos, Despliegues y Eventos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\herramientas\desarrollo\crear-actualizacion-exitosa-demo.ps1
```
