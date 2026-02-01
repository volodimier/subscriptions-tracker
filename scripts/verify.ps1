$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Blue
Write-Host "║              Project Verification Suite                    ║" -ForegroundColor Blue
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Blue
Write-Host ""

Write-Host "[1/9] Running backend tests with coverage..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\backend"
try {
    & .\gradlew.bat test jacocoTestReport --no-daemon -q
    if ($LASTEXITCODE -ne 0) { throw "Backend tests failed" }
} finally {
    Pop-Location
}
Write-Host "✓ Backend tests passed" -ForegroundColor Green
Write-Host "  Coverage: backend\build\reports\jacoco\test\html\index.html" -ForegroundColor Blue
Write-Host ""

Write-Host "[2/9] Installing frontend dependencies..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw "Frontend dependency installation failed" }
} finally {
    Pop-Location
}
Write-Host "✓ Frontend dependencies installed" -ForegroundColor Green
Write-Host ""

Write-Host "[3/9] Running frontend type check..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
try {
    npm run type-check
    if ($LASTEXITCODE -ne 0) { throw "Frontend type check failed" }
} finally {
    Pop-Location
}
Write-Host "✓ Frontend type check passed" -ForegroundColor Green
Write-Host ""

Write-Host "[4/9] Running frontend lint check..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
try {
    npm run lint:check
    if ($LASTEXITCODE -ne 0) { throw "Frontend lint check failed" }
} finally {
    Pop-Location
}
Write-Host "✓ Frontend lint check passed" -ForegroundColor Green
Write-Host ""

Write-Host "[5/9] Running frontend tests with coverage..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
try {
    npm run test:coverage:quiet --silent
    if ($LASTEXITCODE -ne 0) { throw "Frontend tests failed" }
} finally {
    Pop-Location
}
Write-Host "✓ Frontend tests passed" -ForegroundColor Green
Write-Host "  Coverage: frontend\coverage\index.html" -ForegroundColor Blue
Write-Host ""

Push-Location $ProjectRoot
try {
    Write-Host "[6/9] Building test container..." -ForegroundColor Yellow
    docker compose -f docker-compose.verify.yml build -q
    if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }
    Write-Host "✓ Build successful" -ForegroundColor Green

    Write-Host "[7/9] Starting test container..." -ForegroundColor Yellow
    docker compose -f docker-compose.verify.yml up -d subscription-tracker-verify

    Write-Host "[8/9] Waiting for healthy status..." -ForegroundColor Yellow
    for ($i = 1; $i -le 24; $i++) {
        $status = docker inspect --format='{{.State.Health.Status}}' subscription-tracker-verify 2>$null
        if ($status -eq "healthy") {
            Write-Host "✓ Container is healthy" -ForegroundColor Green
            break
        }
        if ($i -eq 24) {
            Write-Host "✗ Container failed to become healthy" -ForegroundColor Red
            docker logs subscription-tracker-verify --tail 20
            docker compose -f docker-compose.verify.yml down
            throw "Container health check failed"
        }
        Start-Sleep -Seconds 5
    }

    Write-Host "[9/9] Stopping test container..." -ForegroundColor Yellow
    docker compose -f docker-compose.verify.yml down -q
    Write-Host "✓ Container stopped" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║              All verifications passed!                     ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "Coverage Reports:" -ForegroundColor Blue
Write-Host "  Backend:  $ProjectRoot\backend\build\reports\jacoco\test\html\index.html"
Write-Host "  Frontend: $ProjectRoot\frontend\coverage\index.html"
Write-Host ""
