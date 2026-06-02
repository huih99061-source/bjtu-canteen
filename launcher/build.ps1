# Self-contained: uses $PSScriptRoot, no parameters needed
$SRC = $PSScriptRoot
$OUT = (Resolve-Path "$PSScriptRoot\..\portable").Path

# Find csc.exe
$csc = $null
@(
    "$env:SystemRoot\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    "$env:SystemRoot\Microsoft.NET\Framework\v4.0.30319\csc.exe"
) | ForEach-Object { if (!$csc -and (Test-Path $_)) { $csc = $_ } }
if (!$csc) { Write-Host "ERROR: csc.exe not found."; exit 1 }
Write-Host "Compiler: $csc"

# Generate icon
Write-Host "Generating icon..."
try {
    Add-Type -AssemblyName System.Drawing
    Add-Type -AssemblyName System.Windows.Forms
    $bmp  = New-Object System.Drawing.Bitmap 64,64
    $g    = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $font = New-Object System.Drawing.Font("Segoe UI Emoji", 48)
    $pt   = New-Object System.Drawing.Point 2,2
    [System.Windows.Forms.TextRenderer]::DrawText($g, [char]::ConvertFromUtf32(0x1F371), $font, $pt, [System.Drawing.Color]::Black)
    $g.Dispose()
    $icon = [System.Drawing.Icon]::FromHandle($bmp.GetHicon())
    $fs   = [System.IO.File]::OpenWrite("$SRC\icon.ico")
    $icon.Save($fs); $fs.Close(); $bmp.Dispose()
    $iconArg = "/win32icon:$SRC\icon.ico"
    Write-Host "Icon OK"
} catch {
    $iconArg = $null
    Write-Host "Icon skipped."
}

# Compile to ASCII temp names, then rename to Unicode
$launcherTmp = "$OUT\launcher_tmp.exe"
$stopperTmp  = "$OUT\stopper_tmp.exe"
$launcherDst = "$OUT\食堂仿真系统.exe"
$stopperDst  = "$OUT\停止系统.exe"

Write-Host "[1/2] Compiling launcher..."
$launcherSrc = "$SRC\Launcher.cs"
if ($iconArg) {
    & $csc /nologo /utf8output /target:exe $iconArg "/out:$launcherTmp" $launcherSrc
} else {
    & $csc /nologo /utf8output /target:exe "/out:$launcherTmp" $launcherSrc
}
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR: Launcher compile failed."; exit 1 }
if (Test-Path $launcherDst) { Remove-Item $launcherDst -Force }
Rename-Item $launcherTmp $launcherDst
Write-Host "OK: 食堂仿真系统.exe"

Write-Host "[2/2] Compiling stopper..."
$stopperSrc = "$SRC\Stopper.cs"
& $csc /nologo /utf8output /target:exe "/out:$stopperTmp" $stopperSrc
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR: Stopper compile failed."; exit 1 }
if (Test-Path $stopperDst) { Remove-Item $stopperDst -Force }
Rename-Item $stopperTmp $stopperDst
Write-Host "OK: 停止系统.exe"

Write-Host "Launcher build complete."
exit 0
