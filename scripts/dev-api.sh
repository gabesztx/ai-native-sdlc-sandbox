#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

cd apps/api
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
