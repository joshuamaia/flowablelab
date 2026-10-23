# Apaga TODOS os dados (volume do PostgreSQL) e recria o ambiente com os dados demonstrativos.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$answer = Read-Host 'Isto remove todas as instâncias, tarefas e solicitações. Continuar? [s/N]'
if ($answer -notmatch '^[sS]$') { Write-Host 'Cancelado.'; exit 0 }

docker compose down -v
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up --build -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Ambiente recriado com dados demonstrativos novos.'
