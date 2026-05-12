#!/usr/bin/env bash
set -e

# ── Paths ────────────────────────────────────────────────────────────────────
BACKEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$(cd "$BACKEND_DIR/../hazina-web" && pwd)"

# ── Colours ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[hazina]${NC} $*"; }
warn()    { echo -e "${YELLOW}[hazina]${NC} $*"; }
error()   { echo -e "${RED}[hazina]${NC} $*"; exit 1; }

# ── Arg parsing ───────────────────────────────────────────────────────────────
MODE="${1:-dev}"   # dev | stop | status

# ── Helpers ───────────────────────────────────────────────────────────────────
wait_for_port() {
  local port=$1 name=$2 attempts=0
  while ! nc -z localhost "$port" 2>/dev/null; do
    ((attempts++))
    [[ $attempts -ge 40 ]] && error "$name did not start on port $port after 40s"
    sleep 1
  done
  info "$name is up on :$port"
}

check_prereqs() {
  for cmd in docker mvn node npm; do
    command -v "$cmd" &>/dev/null || error "'$cmd' not found — please install it first"
  done
}

# ── Stop ──────────────────────────────────────────────────────────────────────
if [[ "$MODE" == "stop" ]]; then
  info "Stopping all Hazina processes…"
  pkill -f "spring-boot:run"  2>/dev/null || true
  pkill -f "next dev"         2>/dev/null || true
  (cd "$BACKEND_DIR" && docker compose stop)
  info "All stopped."
  exit 0
fi

# ── Status ────────────────────────────────────────────────────────────────────
if [[ "$MODE" == "status" ]]; then
  echo ""
  echo "  PostgreSQL  $(nc -z localhost 5433 2>/dev/null && echo '✅ up' || echo '❌ down')  :5433"
  echo "  Backend     $(nc -z localhost 8080 2>/dev/null && echo '✅ up' || echo '❌ down')  :8080"
  echo "  Frontend    $(nc -z localhost 3000 2>/dev/null && echo '✅ up' || echo '❌ down')  :3000"
  echo ""
  exit 0
fi

# ── Dev start ─────────────────────────────────────────────────────────────────
check_prereqs

info "Starting Hazina dev environment…"
echo ""

# 1. PostgreSQL via Docker Compose
info "Starting PostgreSQL…"
(cd "$BACKEND_DIR" && docker compose up -d)
wait_for_port 5433 "PostgreSQL"

# 2. Spring Boot backend
info "Starting Spring Boot backend…"
(cd "$BACKEND_DIR" && mvn spring-boot:run -q) &
BACKEND_PID=$!
wait_for_port 8080 "Backend"

# 3. Next.js frontend
if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  warn "node_modules not found — running npm install…"
  (cd "$FRONTEND_DIR" && npm install --silent)
fi
info "Starting Next.js frontend…"
(cd "$FRONTEND_DIR" && npm run dev) &
FRONTEND_PID=$!
wait_for_port 3000 "Frontend"

echo ""
echo -e "  ${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  ${GREEN}  Hazina is running${NC}"
echo -e "  ${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  App        →  http://localhost:3000"
echo "  API docs   →  http://localhost:8080/swagger-ui.html"
echo "  API base   →  http://localhost:8080/api"
echo ""
echo "  Press Ctrl+C to stop all services"
echo ""

# Keep script alive; kill children on exit
trap "info 'Shutting down…'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; (cd $BACKEND_DIR && docker compose stop); exit 0" SIGINT SIGTERM
wait
