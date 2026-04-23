param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$StrategyId = 1001,
    [int]$Rps = 800,
    [string]$Duration = '5m',
    [int]$PreAllocatedVUs = 300,
    [int]$MaxVUs = 2000,
    [int]$StartUserId = 200000
)

$scriptPath = Join-Path $PSScriptRoot 'raffle-draw-load.js'

k6 run `
  -e BASE_URL=$BaseUrl `
  -e STRATEGY_ID=$StrategyId `
  -e RPS=$Rps `
  -e DURATION=$Duration `
  -e PRE_ALLOCATED_VUS=$PreAllocatedVUs `
  -e MAX_VUS=$MaxVUs `
  -e START_USER_ID=$StartUserId `
  $scriptPath
