param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$StrategyId = 1001,
    [string]$RpsList = '20,50,100,200,400,800',
    [string]$Durations = '1m,1m,1m,1m,1m,3m',
    [int]$StartUserId = 400000,
    [int]$UserStep = 1,
    [int]$PreAllocatedVUs = 20,
    [int]$MaxVUs = 500
)

$scriptPath = Join-Path $PSScriptRoot 'raffle-draw-baseline.js'
$rpsArray = $RpsList.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$durationArray = $Durations.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }

if ($rpsArray.Count -eq 0) {
    throw 'RpsList must not be empty'
}

if ($durationArray.Count -eq 0) {
    throw 'Durations must not be empty'
}

for ($index = 0; $index -lt $rpsArray.Count; $index++) {
    $rps = [int]$rpsArray[$index]
    $duration = if ($index -lt $durationArray.Count) { $durationArray[$index] } else { $durationArray[$durationArray.Count - 1] }
    $stageStartUserId = $StartUserId + ($index * 1000000)

    Write-Host "Running ramp stage $($index + 1): RPS=$rps Duration=$duration StartUserId=$stageStartUserId"

    k6 run `
      -e BASE_URL=$BaseUrl `
      -e STRATEGY_ID=$StrategyId `
      -e RPS=$rps `
      -e DURATION=$duration `
      -e START_USER_ID=$stageStartUserId `
      -e USER_STEP=$UserStep `
      -e PRE_ALLOCATED_VUS=$PreAllocatedVUs `
      -e MAX_VUS=$MaxVUs `
      $scriptPath

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "k6 ramp test failed at stage $($index + 1) with RPS=$rps Duration=$duration exitCode=$LASTEXITCODE"
        exit $LASTEXITCODE
    }

    Write-Host "Completed ramp stage $($index + 1): RPS=$rps Duration=$duration"
}