param(
    [int]$DelaySeconds = 30
)

$tests = @(
    "tests/homepage.spec.js",
    "tests/empty-prompt.spec.js",
    "tests/compare-empty.spec.js",
    "tests/fast-prompt-search.spec.js",
    "tests/fast-cv-upload.spec.js",
    "tests/prompt-search.spec.js",
    "tests/job-details.spec.js",
    "tests/sort-results.spec.js",
    "tests/compare-jobs.spec.js",
    "tests/compare-remove.spec.js",
    "tests/statistics-from-results.spec.js",
    "tests/statistics.spec.js",
    "tests/cv-upload.spec.js",
    "tests/salary-prediction.spec.js"
)

$failed = @()

foreach ($test in $tests) {
    Write-Host ""
    Write-Host "Running $test" -ForegroundColor Cyan
    npx playwright test $test --headed --workers=1

    if ($LASTEXITCODE -ne 0) {
        $failed += $test
        Write-Host "Failed: $test" -ForegroundColor Red
    } else {
        Write-Host "Passed: $test" -ForegroundColor Green
    }

    if ($test -ne $tests[-1]) {
        Write-Host "Waiting $DelaySeconds seconds before the next test..." -ForegroundColor Yellow
        Start-Sleep -Seconds $DelaySeconds
    }
}

Write-Host ""
if ($failed.Count -gt 0) {
    Write-Host "Finished with failures:" -ForegroundColor Red
    $failed | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "All tests passed." -ForegroundColor Green
