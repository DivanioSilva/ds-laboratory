#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: '$1' is not installed or is not available in PATH." >&2
    exit 1
  fi
}

require_command docker

cd "$PROJECT_DIR"

show_usage() {
  cat <<EOF
Usage: $(basename "$0") <command>

Commands:
  build    Compile, test, build the images, and start all services.
  down     Stop services without removing containers, images, networks, or volumes.
  destroy  Stop and remove containers and the network while preserving volumes.
EOF
}

build_and_deploy() {
  require_command mvn

  echo "[1/4] Validating the Docker Compose configuration..."
  docker compose config --quiet

  echo "[2/4] Compiling and testing all Maven modules..."
  mvn clean verify

  echo "[3/4] Building all Docker images..."
  docker compose build

  echo "[4/4] Deploying the services..."
  docker compose up -d --remove-orphans

  echo
  echo "Deployment completed."
  docker compose ps
  echo
  echo "Frontend:  http://localhost:${ANGULAR_PORT:-4200}"
  echo "API:       http://localhost:${SPRING_PORT:-8080}"
  echo "Keycloak:  http://localhost:${KEYCLOAK_PORT:-8081}"
  echo "SMTP UI:   http://localhost:${FAKE_SMTP_WEB_PORT:-8082}"
}

stop_deployment() {
  echo "Stopping all services..."
  docker compose stop
  echo "Services stopped. Containers, images, networks, and volumes were preserved."
}

destroy_deployment() {
  echo "Stopping and removing the services..."
  docker compose down --remove-orphans
  echo "Destroy completed. Volumes and their data were preserved."
}

case "${1:-}" in
  build)
    build_and_deploy
    ;;
  down)
    stop_deployment
    ;;
  destroy)
    destroy_deployment
    ;;
  -h|--help|help)
    show_usage
    ;;
  *)
    show_usage >&2
    exit 1
    ;;
esac
