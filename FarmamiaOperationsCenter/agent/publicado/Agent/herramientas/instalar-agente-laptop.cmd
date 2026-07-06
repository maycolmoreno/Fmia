@echo off
setlocal

net session >nul 2>&1
if not "%errorlevel%"=="0" (
  echo.
  echo Este instalador debe ejecutarse como administrador.
  echo Clic derecho sobre este archivo y seleccione "Ejecutar como administrador".
  echo.
  pause
  exit /b 1
)

set "SCRIPT_DIR=%~dp0"
set "API_URL=http://192.168.0.168:8081"
set "CODIGO_SUCURSAL=FMA001"

echo.
echo Instalador Farmamia Operations Center - Windows Agent
echo.
echo API actual: %API_URL%
set /p API_URL=Ingrese URL API o Enter para conservar: 
if "%API_URL%"=="" set "API_URL=http://192.168.0.168:8081"

echo Codigo sucursal actual: %CODIGO_SUCURSAL%
set /p CODIGO_SUCURSAL=Ingrese codigo sucursal o Enter para conservar: 
if "%CODIGO_SUCURSAL%"=="" set "CODIGO_SUCURSAL=FMA001"

echo.
echo Se creara POS demo si no existe.
echo API: %API_URL%
echo Sucursal: %CODIGO_SUCURSAL%
echo.
pause

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%instalar-agente-laptop.ps1" -UrlApiCentral "%API_URL%" -CodigoSucursal "%CODIGO_SUCURSAL%" -CrearPosDemo

echo.
pause
