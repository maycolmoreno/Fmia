# Admin Panel - Angular

## Responsabilidad

Panel web administrativo para gestionar sucursales, equipos, paquetes POS, campanas, pilotos, grupos, alertas, auditoria, activos y dashboards.

## Modulos iniciales

```txt
auth
dashboard
branches
devices
packages
deployments
alerts
audit
```

## Pantallas MVP

```txt
Login
Dashboard operativo
Listado de equipos
Detalle de equipo
Carga de paquete POS
Listado de paquetes
Crear campana
Estado de campana
Eventos de actualizacion
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
Dashboard operativo
Listado de sucursales
Listado de equipos inventariados
Detalle operativo de equipo
Listado y carga de paquetes POS
Aprobacion, retiro y descarga de paquetes
Listado y creacion de despliegues
Consulta de estado de despliegue
Seleccion de equipos registrados para crear despliegues
Listado de eventos recientes de actualizacion
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
