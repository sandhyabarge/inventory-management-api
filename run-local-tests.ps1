[CmdletBinding()]
param(
    [string]$Test = "AuthUserIntegrationTest"
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path

Push-Location $projectDirectory
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found. Install and start Docker Desktop."
    }

    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Desktop is not running or the current user cannot access it."
    }

    $dockerContext = (docker context show).Trim()
    $detectedDockerHost = (
        docker context inspect $dockerContext --format '{{.Endpoints.docker.Host}}'
    ).Trim()
    $detectedApiVersion = (
        docker version --format '{{.Server.APIVersion}}'
    ).Trim()

    if ([string]::IsNullOrWhiteSpace($detectedDockerHost)) {
        throw "Could not determine the Docker endpoint for context '$dockerContext'."
    }

    $env:DOCKER_HOST = $detectedDockerHost
    if (-not [string]::IsNullOrWhiteSpace($detectedApiVersion)) {
        $env:DOCKER_API_VERSION = $detectedApiVersion
    }

    Write-Host "Docker context: $dockerContext"
    Write-Host "Docker host:    $env:DOCKER_HOST"
    Write-Host "Docker API:     $env:DOCKER_API_VERSION"

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mavenCommand) {
        $intellijMaven =
            "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"
        if (Test-Path -LiteralPath $intellijMaven) {
            $mavenExecutable = $intellijMaven
        } else {
            throw "Maven was not found. Add Maven to PATH or run the shared IntelliJ configuration."
        }
    } else {
        $mavenExecutable = $mavenCommand.Source
    }

    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $localJdk = Join-Path $env:USERPROFILE ".jdks\ms-21.0.11"
        if (Test-Path -LiteralPath (Join-Path $localJdk "bin\java.exe")) {
            $env:JAVA_HOME = $localJdk
        }
    }

    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        throw "JAVA_HOME is not configured. Select or install JDK 21."
    }

    Write-Host "Running test:   $Test"
    & $mavenExecutable `
        --batch-mode `
        --no-transfer-progress `
        "-Dapi.version=$detectedApiVersion" `
        "-Dtest=$Test" `
        test

    if ($LASTEXITCODE -ne 0) {
        throw "Maven tests failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
