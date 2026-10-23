#!/usr/bin/env bash
# Sobe o FlowableLab completo (Linux / macOS / Git Bash).
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { cp .env.example .env; echo "Arquivo .env criado a partir de .env.example"; }

docker compose up --build -d --wait
echo
echo "FlowableLab em execução:"
echo "  Interface:  http://localhost:${FRONTEND_PORT:-4200}"
echo "  Swagger:    http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html"
