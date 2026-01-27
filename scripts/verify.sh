#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║              Project Verification Suite                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${YELLOW}[1/7] Running backend tests with coverage...${NC}"
(cd "$PROJECT_ROOT/backend" && ./gradlew test jacocoTestReport --no-daemon -q)
echo -e "${GREEN}✓ Backend tests passed${NC}"
echo -e "${BLUE}  Coverage: backend/build/reports/jacoco/test/html/index.html${NC}"
echo ""

echo -e "${YELLOW}[2/7] Installing frontend dependencies...${NC}"
(cd "$PROJECT_ROOT/frontend" && npm ci)
echo -e "${GREEN}✓ Frontend dependencies installed${NC}"
echo ""

echo -e "${YELLOW}[3/7] Running frontend tests with coverage...${NC}"
(cd "$PROJECT_ROOT/frontend" && npm run test:coverage --silent)
echo -e "${GREEN}✓ Frontend tests passed${NC}"
echo -e "${BLUE}  Coverage: frontend/coverage/index.html${NC}"
echo ""

echo -e "${YELLOW}[4/7] Building test container...${NC}"
docker compose -f "$PROJECT_ROOT/docker-compose.test.yml" build -q
echo -e "${GREEN}✓ Build successful${NC}"

echo -e "${YELLOW}[5/7] Starting test container...${NC}"
docker compose -f "$PROJECT_ROOT/docker-compose.test.yml" up -d subscription-tracker-test

echo -e "${YELLOW}[6/7] Waiting for healthy status...${NC}"
for i in {1..24}; do
  status=$(docker inspect --format='{{.State.Health.Status}}' subscription-tracker-test 2>/dev/null)
  if [ "$status" = "healthy" ]; then
    echo -e "${GREEN}✓ Container is healthy${NC}"
    break
  fi
  if [ $i -eq 24 ]; then
    echo -e "${RED}✗ Container failed to become healthy${NC}"
    docker logs subscription-tracker-test --tail 20
    docker compose -f "$PROJECT_ROOT/docker-compose.test.yml" down
    exit 1
  fi
  sleep 5
done

echo -e "${YELLOW}[7/7] Stopping test container...${NC}"
docker compose -f "$PROJECT_ROOT/docker-compose.test.yml" down
echo -e "${GREEN}✓ Container stopped${NC}"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              All verifications passed!                     ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Coverage Reports:${NC}"
echo -e "  Backend:  ${PROJECT_ROOT}/backend/build/reports/jacoco/test/html/index.html"
echo -e "  Frontend: ${PROJECT_ROOT}/frontend/coverage/index.html"
echo ""
