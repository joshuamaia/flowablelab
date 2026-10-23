#!/usr/bin/env bash
# Apaga TODOS os dados (volume do PostgreSQL) e recria o ambiente com os dados demonstrativos.
set -euo pipefail
cd "$(dirname "$0")/.."

read -r -p "Isto remove todas as instâncias, tarefas e solicitações. Continuar? [s/N] " answer
[[ "$answer" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

docker compose down -v
[ -f .env ] || cp .env.example .env
docker compose up --build -d --wait
echo "Ambiente recriado com dados demonstrativos novos."
