# Monitoring Stack Deployment Script
# This script builds and deploys the entire monitoring stack with all microservices

param(
    [string]$Action = "all",
    [switch]$SkipBuild = $false,
    [switch]$SkipTests = $true
)

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = "deployment_$timestamp.log"

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $logMessage = "[$(Get-Date -Format 'HH:mm:ss')] [$Level] $Message"
    Write-Host $logMessage
    Add-Content -Path $logFile -Value $logMessage
}

function Test-Docker {
    Write-Log "Checking Docker installation..."
    try {
        $dockerVersion = docker --version
        Write-Log "✅ Docker found: $dockerVersion"
        return $true
    }
    catch {
        Write-Log "❌ Docker not found. Please install Docker Desktop." "ERROR"
        return $false
    }
}

function Clean-Build {
    Write-Log "Cleaning previous Maven builds..."
    mvn clean -DskipTests -q
    Write-Log "✅ Maven clean complete"
}

function Build-Services {
    if ($SkipBuild) {
        Write-Log "⏭️  Skipping Maven build (--SkipBuild flag set)"
        return
    }

    Write-Log "Building all services with Maven..."
    $testFlag = if ($SkipTests) { "-DskipTests" } else { "" }

    Write-Log "Running: mvn package $testFlag -q"
    mvn package $testFlag -q

    if ($LASTEXITCODE -ne 0) {
        Write-Log "❌ Maven build failed!" "ERROR"
        exit 1
    }
    Write-Log "✅ All services built successfully"
}

function Stop-Containers {
    Write-Log "Stopping existing containers..."
    docker-compose down -v 2>&1 | Tee-Object -FilePath $logFile -Append | ForEach-Object {
        if ($_ -match "error|Error") {
            Write-Log $_ "WARN"
        }
    }
    Write-Log "✅ Containers stopped"
}

function Build-Images {
    Write-Log "Building Docker images..."
    docker-compose build --no-cache 2>&1 | Tee-Object -FilePath $logFile -Append

    if ($LASTEXITCODE -ne 0) {
        Write-Log "❌ Docker build failed!" "ERROR"
        exit 1
    }
    Write-Log "✅ Docker images built"
}

function Start-Services {
    Write-Log "Starting all services..."
    docker-compose up -d

    if ($LASTEXITCODE -ne 0) {
        Write-Log "❌ Failed to start services!" "ERROR"
        exit 1
    }

    Write-Log "⏳ Waiting for services to be ready (30 seconds)..."
    Start-Sleep -Seconds 30

    Write-Log "Checking service status..."
    docker-compose ps
}

function Verify-Services {
    Write-Log "Verifying services..."

    $services = @(
        @{Name = "api-gateway"; Port = 8062},
        @{Name = "auth-service"; Port = 8063},
        @{Name = "wallet-service"; Port = 8065},
        @{Name = "config-server"; Port = 8060},
        @{Name = "eureka-server"; Port = 8061},
        @{Name = "prometheus"; Port = 9090},
        @{Name = "grafana"; Port = 3000}
    )

    $allHealthy = $true

    foreach ($service in $services) {
        try {
            $response = curl -s -o /dev/null -w "%{http_code}" "http://localhost:$($service.Port)/actuator/health" -ErrorAction SilentlyContinue
            if ($response -eq "200") {
                Write-Log "✅ $($service.Name) is healthy"
            } else {
                Write-Log "⚠️  $($service.Name) status: $response" "WARN"
                $allHealthy = $false
            }
        }
        catch {
            Write-Log "❌ $($service.Name) not responding" "WARN"
            $allHealthy = $false
        }
    }

    return $allHealthy
}

function Test-Prometheus {
    Write-Log "Testing Prometheus endpoints..."

    $endpoints = @(
        @{Service = "api-gateway"; Url = "http://localhost:8062/actuator/prometheus"},
        @{Service = "auth-service"; Url = "http://localhost:8063/actuator/prometheus"},
        @{Service = "wallet-service"; Url = "http://localhost:8065/actuator/prometheus"},
        @{Service = "config-server"; Url = "http://localhost:8060/actuator/prometheus"},
        @{Service = "eureka-server"; Url = "http://localhost:8061/actuator/prometheus"}
    )

    foreach ($endpoint in $endpoints) {
        try {
            $response = curl -s -o /dev/null -w "%{http_code}" $endpoint.Url -ErrorAction SilentlyContinue
            if ($response -eq "200") {
                Write-Log "✅ $($endpoint.Service) prometheus endpoint working"
            } else {
                Write-Log "❌ $($endpoint.Service) returned $response" "ERROR"
            }
        }
        catch {
            Write-Log "❌ $($endpoint.Service) endpoint failed" "ERROR"
        }
    }
}

function Check-Prometheus-Targets {
    Write-Log "Checking Prometheus targets (this may take 1-2 minutes)..."
    Write-Log "URL: http://localhost:9090/targets"
    Write-Log ""
    Write-Log "Expected: All services should show UP (green)"
    Write-Log "If services show DOWN (red), check docker logs:"
    Write-Log "  docker-compose logs <service-name>"
}

function Show-Access-URLs {
    Write-Log ""
    Write-Log "========================================="
    Write-Log "🎉 Deployment Complete!"
    Write-Log "========================================="
    Write-Log ""
    Write-Log "📊 Access URLs:"
    Write-Log "  Prometheus: http://localhost:9090"
    Write-Log "  Prometheus Targets: http://localhost:9090/targets"
    Write-Log "  Grafana: http://localhost:3000"
    Write-Log "  Grafana Login: admin / admin123"
    Write-Log ""
    Write-Log "🔍 Verification Steps:"
    Write-Log "  1. Open http://localhost:9090/targets"
    Write-Log "  2. All 11 services should show UP (green)"
    Write-Log "  3. Open http://localhost:3000"
    Write-Log "  4. Configure Prometheus data source if needed"
    Write-Log "  5. Create dashboards with PromQL queries"
    Write-Log ""
    Write-Log "📝 Useful PromQL Queries:"
    Write-Log "  up                              - Service health"
    Write-Log "  rate(http_server_requests_seconds_count[5m])  - Request rate"
    Write-Log "  jvm_memory_used_bytes{area=\"heap\"}  - Heap memory"
    Write-Log ""
    Write-Log "📋 Service Ports:"
    Write-Log "  API Gateway: 8062"
    Write-Log "  Auth Service: 8063"
    Write-Log "  User KYC Service: 8064"
    Write-Log "  Wallet Service: 8065"
    Write-Log "  Rewards Service: 8066"
    Write-Log "  Admin Service: 8067"
    Write-Log "  Transaction Service: 8068"
    Write-Log "  Notification Service: 8069"
    Write-Log "  Integration Service: 8070"
    Write-Log "  Eureka Server: 8061"
    Write-Log "  Config Server: 8060"
    Write-Log "  Prometheus: 9090"
    Write-Log "  Grafana: 3000"
    Write-Log ""
    Write-Log "Deployment log saved to: $logFile"
    Write-Log "========================================="
}

# Main execution
function Main {
    Write-Log "Starting Monitoring Stack Deployment..."
    Write-Log "Action: $Action"
    Write-Log ""

    if (-not (Test-Docker)) {
        exit 1
    }

    switch ($Action) {
        "all" {
            Clean-Build
            Build-Services
            Stop-Containers
            Build-Images
            Start-Services
            Write-Log "⏳ Waiting 15 seconds for services to stabilize..."
            Start-Sleep -Seconds 15
            Verify-Services
            Test-Prometheus
            Check-Prometheus-Targets
            Show-Access-URLs
        }
        "build" {
            Clean-Build
            Build-Services
        }
        "docker" {
            Stop-Containers
            Build-Images
            Start-Services
            Verify-Services
            Show-Access-URLs
        }
        "start" {
            Start-Services
        }
        "stop" {
            Stop-Containers
        }
        "verify" {
            Verify-Services
            Test-Prometheus
            Check-Prometheus-Targets
        }
        "logs" {
            Write-Log "Showing docker-compose logs (Ctrl+C to exit)..."
            docker-compose logs -f
        }
        "status" {
            Write-Log "Current container status:"
            docker-compose ps
        }
        default {
            Write-Log "Unknown action: $Action" "ERROR"
            Write-Log "Valid actions: all, build, docker, start, stop, verify, logs, status"
            exit 1
        }
    }

    Write-Log ""
    Write-Log "✅ Script completed successfully"
}

# Execute main
Main

