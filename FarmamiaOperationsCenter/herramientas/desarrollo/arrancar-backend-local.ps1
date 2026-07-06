# Arranca el backend localmente con mvnw spring-boot:run, cargando
# FARMAMIA_DB_PASSWORD / FARMAMIA_JWT_SECRET desde backend\.env.local.ps1
# (gitignored). Si no existe, la crea a partir de .env.local.ps1.example
# pidiendo los valores una sola vez.

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backend = Join-Path $raiz "backend"
$envLocal = Join-Path $backend ".env.local.ps1"
$envEjemplo = Join-Path $backend ".env.local.ps1.example"

if (-not (Test-Path $envLocal)) {
    Write-Host "No existe $envLocal - se creara ahora (no se sube al repo)."

    $dbPassword = Read-Host "Password de tu PostgreSQL local (FARMAMIA_DB_PASSWORD)" -AsSecureString
    $dbPasswordPlano = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)
    )

    $caracteres = (48..57) + (65..90) + (97..122)
    $jwtSecret = -join ((1..40) | ForEach-Object { [char]($caracteres | Get-Random) })

    @"
# Generado por arrancar-backend-local.ps1 - NO subir al repo (ya esta en .gitignore).
`$env:FARMAMIA_DB_PASSWORD = "$dbPasswordPlano"
`$env:FARMAMIA_JWT_SECRET = "$jwtSecret"
"@ | Set-Content -Path $envLocal -Encoding UTF8

    Write-Host "Creado $envLocal con un FARMAMIA_JWT_SECRET generado aleatoriamente."
}

. $envLocal

Push-Location $backend
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
