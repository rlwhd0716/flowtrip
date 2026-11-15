param(
    [string]$DeviceId = '',
    [ValidateRange(1024, 65535)][int]$Port = 8787,
    [switch]$ServerOnly
)
$ErrorActionPreference = 'Stop'
$repoPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$healthUrl = "http://127.0.0.1:$Port/health"
function Test-PlaceProxy {
    try { return (Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2).service -eq 'flowtrip-place-proxy' }
    catch { return $false }
}
if (-not (Test-PlaceProxy)) {
    $nodePath = (Get-Command node -ErrorAction Stop).Source
    $proxyPidOutput = & $nodePath (Join-Path $PSScriptRoot 'launch.mjs') "$Port"
    if ($LASTEXITCODE -ne 0 -or "$proxyPidOutput" -notmatch '^\d+$') { throw 'Place proxy launcher failed.' }
    $proxyProcess = Get-Process -Id ([int]$proxyPidOutput) -ErrorAction Stop
    for ($attempt = 0; $attempt -lt 20 -and -not (Test-PlaceProxy); $attempt++) {
        if ($proxyProcess.HasExited) { throw 'Place proxy stopped. Check tools/place-proxy/proxy.stderr.log.' }
        Start-Sleep -Milliseconds 250
    }
    if (-not (Test-PlaceProxy)) { throw 'Place proxy did not become ready. Check its log and port.' }
    Write-Output "Started local place proxy (PID $($proxyProcess.Id))."
} else { Write-Output 'Local place proxy is already running.' }

if ($ServerOnly) {
    Write-Output "Ready: local place proxy on port $Port. Emulator uses 10.0.2.2 (no adb reverse required)."
    return
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCommand) { $adbPath = $adbCommand.Source }
else {
    $sdkProperty = Get-Content -LiteralPath (Join-Path $repoPath 'local.properties') | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if (-not $sdkProperty) { throw 'Android SDK not found. Add adb to PATH or set sdk.dir in local.properties.' }
    $sdkPath = ($sdkProperty -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\')
    $adbPath = Join-Path $sdkPath 'platform-tools/adb.exe'
}
if (-not $DeviceId) {
    $connected = @(& $adbPath devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if ($connected.Count -ne 1) { throw 'Connect exactly one Android device, or specify -DeviceId.' }
    $DeviceId = $connected[0]
}
& $adbPath -s $DeviceId reverse "tcp:$Port" "tcp:$Port"
if ($LASTEXITCODE -ne 0) { throw 'adb reverse failed.' }
$mapping = & $adbPath -s $DeviceId reverse --list
if (-not ($mapping | Select-String "tcp:$Port tcp:$Port")) { throw 'adb reverse mapping was not confirmed.' }

$settingsPath = Join-Path $repoPath 'place-proxy.local.properties'
$setting = "debugPlaceProxyUrl=http://127.0.0.1:$Port"
$settings = if (Test-Path -LiteralPath $settingsPath) { [System.IO.File]::ReadAllText($settingsPath) } else { '' }
if ($settings -match '(?m)^debugPlaceProxyUrl=.*$') { $settings = [regex]::Replace($settings, '(?m)^debugPlaceProxyUrl=.*$', $setting) }
else { $settings = $settings.TrimEnd() + "`n$setting`n" }
[System.IO.File]::WriteAllText($settingsPath, $settings, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Ready: $DeviceId -> adb reverse -> PC place proxy."
Write-Output "Debug URL: http://127.0.0.1:$Port (rebuild/reinstall the app if its previous URL was 10.0.2.2)."
