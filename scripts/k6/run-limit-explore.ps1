param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$StrategyId = 1001,
    [string]$RpsList = '200,300,400,500,600,700,800,900,1000,1200',
    [string]$Duration = '1m',
    [int]$StartUserId = 500000,
    [int]$UserStep = 1,
    [int]$PreAllocatedVUs = 100,
    [int]$MaxVUs = 4000,
    [double]$P95TargetMs = 200,
    [double]$ErrorRateTarget = 0.01,
    [double]$DroppedRatioTarget = 0.01,
    [switch]$ContinueAfterUnstable
)

$scriptPath = Join-Path $PSScriptRoot 'raffle-draw-limit-stage.js'
$outputDir = Join-Path $PSScriptRoot 'out'
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$rpsArray = $RpsList.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
if ($rpsArray.Count -eq 0) {
    throw 'RpsList must not be empty'
}

$bestStableRps = 0
$results = @()

foreach ($rawRps in $rpsArray) {
    $rps = [int]$rawRps
    $stageStartUserId = $StartUserId + ($rps * 10000)
    $summaryFile = Join-Path $outputDir ("limit-rps-$rps.json")

    Write-Host "Running limit stage: RPS=$rps Duration=$Duration"

    k6 run `
      --summary-export $summaryFile `
      -e BASE_URL=$BaseUrl `
      -e STRATEGY_ID=$StrategyId `
      -e RPS=$rps `
      -e DURATION=$Duration `
      -e START_USER_ID=$stageStartUserId `
      -e USER_STEP=$UserStep `
      -e PRE_ALLOCATED_VUS=$PreAllocatedVUs `
      -e MAX_VUS=$MaxVUs `
      $scriptPath

    if (-not (Test-Path $summaryFile)) {
        throw "summary export not found: $summaryFile"
    }

    $summary = Get-Content $summaryFile -Raw | ConvertFrom-Json
    $metrics = $summary.metrics

    $httpFailed = $metrics.http_req_failed
    $failedPasses = if ($httpFailed.PSObject.Properties.Name -contains 'passes') { [double]$httpFailed.passes } else { 0 }
    $failedFails = if ($httpFailed.PSObject.Properties.Name -contains 'fails') { [double]$httpFailed.fails } else { 0 }
    $failedTotal = $failedPasses + $failedFails
    # For http_req_failed metric, "passes" is the number of failed requests (rate=true).
    $errorRate = if ($failedTotal -gt 0) { $failedPasses / $failedTotal } else { 0 }

    $httpDuration = $metrics.http_req_duration
    $p95 = if ($httpDuration.PSObject.Properties.Name -contains 'p(95)') { [double]$httpDuration.'p(95)' } else { 0 }

    $droppedCount = if ($metrics.PSObject.Properties.Name -contains 'dropped_iterations') { [double]$metrics.dropped_iterations.count } else { 0 }
    $iterationCount = if ($metrics.PSObject.Properties.Name -contains 'iterations') { [double]$metrics.iterations.count } else { 0 }
    $actualRps = if ($metrics.PSObject.Properties.Name -contains 'http_reqs') { [double]$metrics.http_reqs.rate } else { 0 }

    $denominator = $iterationCount + $droppedCount
    $droppedRatio = if ($denominator -gt 0) { $droppedCount / $denominator } else { 0 }

    $isStable = ($errorRate -le $ErrorRateTarget) -and ($p95 -le $P95TargetMs) -and ($droppedRatio -le $DroppedRatioTarget)
    if ($isStable) {
        $bestStableRps = $rps
    }

    $row = [PSCustomObject]@{
        targetRps = $rps
        actualRps = [Math]::Round($actualRps, 2)
        p95Ms = [Math]::Round($p95, 2)
        errorRate = [Math]::Round($errorRate, 4)
        droppedRatio = [Math]::Round($droppedRatio, 4)
        stable = $isStable
        summary = $summaryFile
    }
    $results += $row

    if (-not $isStable) {
        Write-Warning "Stage unstable at RPS=$rps (p95=$($row.p95Ms)ms errorRate=$($row.errorRate) droppedRatio=$($row.droppedRatio))"
        if (-not $ContinueAfterUnstable.IsPresent) {
            break
        }
    }
}

Write-Host "`n===== Limit Exploration Result ====="
$results | Format-Table -AutoSize
Write-Host "Max stable RPS: $bestStableRps"

if ($bestStableRps -eq 0) {
    exit 2
}