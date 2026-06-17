@echo off
setlocal

set "BASE_DIR=%~dp0"
set "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
set "PROPERTIES_FILE=%WRAPPER_DIR%\maven-wrapper.properties"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"
set "MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "DISTRIBUTION_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%PROPERTIES_FILE%" (
  echo No existe %PROPERTIES_FILE%.
  exit /b 1
)

if not exist "%MAVEN_CMD%" (
  echo Descargando Apache Maven %MAVEN_VERSION% para este proyecto...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_ZIP%'; Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
  if errorlevel 1 (
    echo No se pudo descargar Maven. Verifique acceso a internet o instale Maven localmente.
    exit /b 1
  )
)

call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
