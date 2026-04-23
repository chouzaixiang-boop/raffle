param(
    [string]$LogFile = 'logs/raffle-app.log',
    [int]$TailLines = 0
)

function Get-Percentile {
    param(
        [double[]]$Values,
        [double]$Percentile
    )

    if (-not $Values -or $Values.Count -eq 0) {
        return [double]::NaN
    }

    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [double]$sorted[$index]
}

if (-not (Test-Path $LogFile)) {
    throw "Log file not found: $LogFile"
}

$lines = if ($TailLines -gt 0) {
    Get-Content -Path $LogFile -Tail $TailLines
} else {
    Get-Content -Path $LogFile
}

$timingLines = $lines | Where-Object { $_ -match 'raffle_draw_timing' }
if (-not $timingLines -or $timingLines.Count -eq 0) {
    Write-Host 'No raffle_draw_timing lines found.'
    exit 0
}

$totalValues = New-Object System.Collections.Generic.List[double]
$stageMap = @{}

foreach ($line in $timingLines) {
    if ($line -match 'totalMs=(\d+)') {
        $totalValues.Add([double]$Matches[1]) | Out-Null
    }

    if ($line -match 'stageMs=([^\s]+)') {
        $stageText = $Matches[1]
        $pairs = $stageText.Split(',') | Where-Object { $_ -match '=' }
        foreach ($pair in $pairs) {
            $parts = $pair.Split('=')
            if ($parts.Count -ne 2) {
                continue
            }
            $stage = $parts[0]
            $value = 0.0
            if (-not [double]::TryParse($parts[1], [ref]$value)) {
                continue
            }
            if (-not $stageMap.ContainsKey($stage)) {
                $stageMap[$stage] = New-Object System.Collections.Generic.List[double]
            }
            $stageMap[$stage].Add($value) | Out-Null
        }
    }
}

Write-Host "Sample count: $($timingLines.Count)"

if ($totalValues.Count -gt 0) {
    $totalAvg = [Math]::Round((($totalValues | Measure-Object -Average).Average), 2)
    $totalP95 = [Math]::Round((Get-Percentile -Values $totalValues.ToArray() -Percentile 95), 2)
    $totalP99 = [Math]::Round((Get-Percentile -Values $totalValues.ToArray() -Percentile 99), 2)
    Write-Host "TOTAL  avg=${totalAvg}ms p95=${totalP95}ms p99=${totalP99}ms"
}

Write-Host 'STAGES:'
foreach ($stage in ($stageMap.Keys | Sort-Object)) {
    $values = $stageMap[$stage].ToArray()
    $avg = [Math]::Round((($values | Measure-Object -Average).Average), 2)
    $p95 = [Math]::Round((Get-Percentile -Values $values -Percentile 95), 2)
    $p99 = [Math]::Round((Get-Percentile -Values $values -Percentile 99), 2)
    Write-Host ("  {0,-20} avg={1,8}ms p95={2,8}ms p99={3,8}ms" -f $stage, $avg, $p95, $p99)
}
