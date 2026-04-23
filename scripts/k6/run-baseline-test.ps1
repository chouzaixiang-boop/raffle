param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$StrategyId = 1001,
    [string]$Durations = '1m,2m,3m',
    [string]$RpsList = '1,5,20',
    [int]$StartUserId = 300000,
    [int]$UserStep = 1,
    [int]$PreAllocatedVUs = 10,
    [int]$MaxVUs = 100
)

$scriptPath = Join-Path $PSScriptRoot 'raffle-draw-baseline.js'
$durationArray = $Durations.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$rpsArray = $RpsList.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }

if ($durationArray.Count -eq 0) {
    throw 'Durations must not be empty'
}

if ($rpsArray.Count -eq 0) {
    throw 'RpsList must not be empty'
}

for ($index = 0; $index -lt $rpsArray.Count; $index++) {
    $rps = [int]$rpsArray[$index]
    $duration = if ($index -lt $durationArray.Count) { $durationArray[$index] } else { $durationArray[$durationArray.Count - 1] }

    Write-Host "Running baseline test: RPS=$rps Duration=$duration"

    k6 run `
      -e BASE_URL=$BaseUrl `
      -e STRATEGY_ID=$StrategyId `
      -e RPS=$rps `
      -e DURATION=$duration `
      -e START_USER_ID=$StartUserId `
      -e USER_STEP=$UserStep `
      -e PRE_ALLOCATED_VUS=$PreAllocatedVUs `
      -e MAX_VUS=$MaxVUs `
      $scriptPath

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "k6 baseline test failed at RPS=$rps Duration=$duration with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }

    Write-Host "Completed baseline test: RPS=$rps Duration=$duration"
}