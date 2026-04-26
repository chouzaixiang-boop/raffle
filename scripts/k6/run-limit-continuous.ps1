param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$StrategyId = 1001,
    [int]$StartRps = 50,
    [string]$RpsLadder = '100,200,300,400,500,600,700,800,900,1000',
    [string]$StepDuration = '3m',
    [string]$HoldDuration = '24h',
    [int]$StartUserId = 900000,
    [int]$UserStep = 1,
    [int]$PreAllocatedVUs = 800,
    [int]$MaxVUs = 8000,
    [string]$ReqTimeout = '5s'
)

$scriptPath = Join-Path $PSScriptRoot 'raffle-draw-limit-continuous.js'

Write-Host 'Starting continuous limit test (single k6 process, no stage restarts).'
Write-Host 'Press Ctrl+C when you want to stop.'
Write-Host "BaseUrl=$BaseUrl StrategyId=$StrategyId StartRps=$StartRps Ladder=$RpsLadder"
Write-Host "StepDuration=$StepDuration HoldDuration=$HoldDuration PreAllocatedVUs=$PreAllocatedVUs MaxVUs=$MaxVUs"

k6 run `
  -e BASE_URL=$BaseUrl `
  -e STRATEGY_ID=$StrategyId `
  -e START_RPS=$StartRps `
  -e RPS_LADDER=$RpsLadder `
  -e STEP_DURATION=$StepDuration `
  -e HOLD_DURATION=$HoldDuration `
  -e START_USER_ID=$StartUserId `
  -e USER_STEP=$UserStep `
  -e PRE_ALLOCATED_VUS=$PreAllocatedVUs `
  -e MAX_VUS=$MaxVUs `
  -e REQ_TIMEOUT=$ReqTimeout `
  $scriptPath
