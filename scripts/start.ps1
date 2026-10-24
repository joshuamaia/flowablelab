# Sobe o FlowableLab completo (Windows PowerShell).
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
    Write-Host 'Arquivo .env criado a partir de .env.example'
}

docker compose up --build -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ''
Write-Host 'FlowableLab em execução:'
Write-Host '  Interface:  http://localhost:4200'
Write-Host '  Swagger:    http://localhost:4200/swagger-ui.html'
