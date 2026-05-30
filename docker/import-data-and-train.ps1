$ErrorActionPreference = "Stop"

$composeFiles = @(
    "-f", "docker/docker-compose.yml",
    "-f", "docker/docker-compose.dev.yml",
    "--profile", "import"
)

docker compose @composeFiles build data-importer
if ($LASTEXITCODE -ne 0) {
    throw "Dataset importer build failed. Import and salary model training were not started."
}

docker compose @composeFiles run --rm data-importer
if ($LASTEXITCODE -ne 0) {
    throw "Dataset import failed. Salary model training was not started."
}

docker compose @composeFiles run --rm --no-deps salary-trainer
if ($LASTEXITCODE -ne 0) {
    throw "Salary model training failed."
}

Write-Host "Dataset import and salary model training completed successfully."
