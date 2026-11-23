<#
.SYNOPSIS
  Read-only check for the EFN Client (efn.lol) Minecraft mod stealer on Windows.

.DESCRIPTION
  Looks for every artifact the dropper/stage 2/stage 3 leave behind, scans Minecraft
  instance folders for infected jars, and lists running malware processes.
  It changes nothing: no deletion, no process killing, no network access.

  Run:  powershell -ExecutionPolicy Bypass -File .\Check-EfnInfection.ps1
#>
[CmdletBinding()]
param(
    # Extra folders to scan for mod jars (e.g. a custom launcher's instances dir)
    [string[]]$ExtraModDirs = @()
)

$ErrorActionPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$findings = New-Object System.Collections.Generic.List[string]
function Hit([string]$msg) { $findings.Add($msg); Write-Host "[!] $msg" -ForegroundColor Red }
function Ok([string]$msg)  { Write-Host "[ok] $msg" -ForegroundColor Green }

$KnownSha256 = @{
    '581870564FA9E71C09A6824B39CC75EBBD81E3C3690F154B3EF6AA08A4BAB40B' = 'efn-client-1.2.0.jar (dropper)'
    'B9E5D53D59E44DFB81C2C10607B4FAEB02090941CEC90FBDFBCEF9B109172692' = 'p.jar (decrypted stage 2)'
}

# --- 1. stage 2/3 working directory --------------------------------------------------------
$work = Join-Path $env:LOCALAPPDATA 'Microsoft\Windows\NtProfileIndex'
if (Test-Path -LiteralPath $work) {
    Hit "Stage 2/3 folder exists: $work"
    Get-ChildItem -LiteralPath $work -Force -Recurse -Depth 1 |
        Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize | Out-String | Write-Host
    foreach ($log in '_spawn.log', '_stealer.log') {
        $p = Join-Path $work $log
        if (Test-Path -LiteralPath $p) {
            $first = Get-Content -LiteralPath $p -TotalCount 1
            Hit "$log present (first line: $first)"
        }
    }
    if (Test-Path -LiteralPath (Join-Path $work 'AppHost\main.py')) { Hit 'Python stealer (AppHost\main.py) was downloaded - assume it ran' }
} else { Ok "No $work" }

# --- 2. dropped p.jar in %TEMP%\efn* ----------------------------------------------------------
$temps = Get-ChildItem -LiteralPath $env:TEMP -Directory -Filter 'efn*' -Force
$found = $false
foreach ($d in $temps) {
    $pj = Join-Path $d.FullName 'p.jar'
    if (Test-Path -LiteralPath $pj) {
        $found = $true
        $h = (Get-FileHash -LiteralPath $pj -Algorithm SHA256).Hash
        $tag = if ($KnownSha256.ContainsKey($h)) { $KnownSha256[$h] } else { 'unknown variant' }
        Hit "Dropped stage 2: $pj  sha256=$h  [$tag]  created $($d.CreationTime)"
    }
}
if (-not $found) { Ok 'No %TEMP%\efn*\p.jar' }

# --- 3. running processes ---------------------------------------------------------------------
$procs = Get-CimInstance Win32_Process | Where-Object {
    $_.CommandLine -match 'net\.efn\.loader\.' -or
    $_.ExecutablePath -like '*\Microsoft\Windows\NtProfileIndex\*' -or
    ($_.CommandLine -match 'main\.py' -and $_.CommandLine -match '--prefireId')
}
if ($procs) {
    foreach ($p in $procs) { Hit "Running: PID $($p.ProcessId) $($p.Name)  $($p.CommandLine)" }
} else { Ok 'No running EFN processes' }

# --- 4. infected jars in Minecraft instance folders ------------------------------------------
$roots = @(
    "$env:APPDATA\.minecraft",
    "$env:APPDATA\PrismLauncher\instances",
    "$env:APPDATA\MultiMC\instances",
    "$env:APPDATA\ModrinthApp\profiles",
    "$env:APPDATA\com.modrinth.theseus\profiles",
    "$env:APPDATA\ATLauncher\instances",
    "$env:USERPROFILE\curseforge\minecraft\Instances",
    "$env:APPDATA\.feather",
    "$env:USERPROFILE\.lunarclient",
    "$env:USERPROFILE\Downloads",
    "$env:USERPROFILE\Desktop"
) + $ExtraModDirs

$jars = foreach ($r in $roots) {
    if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Recurse -Filter '*.jar' -File -Force }
}
$scanned = 0
foreach ($j in $jars) {
    $scanned++
    $h = (Get-FileHash -LiteralPath $j.FullName -Algorithm SHA256).Hash
    if ($KnownSha256.ContainsKey($h)) { Hit "Known-bad jar: $($j.FullName) [$($KnownSha256[$h])]"; continue }
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
        $names = $zip.Entries | ForEach-Object FullName
        $zip.Dispose()
        if ($names -contains 'net/efn/agent/AgentInit.class' -or $names -contains 'assets/efn/data/runtime.dat') {
            Hit "EFN dropper variant: $($j.FullName)"
        } elseif (($names | Where-Object { $_ -like 'net/efn/loader/*' }).Count -ge 3) {
            Hit "EFN stage 2 jar: $($j.FullName)"
        }
    } catch { }
}
Ok "Scanned $scanned jar(s)"

# --- 5. EFN alt-manager tokens (high-value target for the stealer) ---------------------------
foreach ($r in $roots) {
    if (-not (Test-Path -LiteralPath $r)) { continue }
    Get-ChildItem -LiteralPath $r -Recurse -Filter 'accounts.json' -File -Force |
        Where-Object { $_.DirectoryName -like '*\config\efn' } |
        ForEach-Object { Hit "EFN account store with plaintext Microsoft refresh tokens: $($_.FullName)" }
}

# --- summary ------------------------------------------------------------------------------
Write-Host ''
if ($findings.Count -eq 0) {
    Write-Host 'No EFN artifacts found.' -ForegroundColor Green
} else {
    Write-Host "$($findings.Count) finding(s). If the stage 2/3 folder or logs exist, the stealer ran:" -ForegroundColor Yellow
    Write-Host '  - from a CLEAN device, change the Microsoft account password (kills Minecraft sessions) and every'
    Write-Host '    password saved in browsers on this PC; log out all Discord sessions; move any crypto funds.'
    Write-Host '  - remove the mod jar(s), the NtProfileIndex folder and %TEMP%\efn* folders, then reinstall Windows'
    Write-Host '    or at least run a full AV scan - the Python stage is downloaded at runtime and may differ.'
}
