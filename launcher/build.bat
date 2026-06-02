@echo off
chcp 65001 > nul
setlocal

echo ==================================================
echo   编译启动器（使用 Windows 内置 .NET 编译器）
echo ==================================================
echo.

:: ── 查找内置 csc.exe ──────────────────────────────
set "CSC="
for %%d in (
    "%SystemRoot%\Microsoft.NET\Framework64\v4.0.30319"
    "%SystemRoot%\Microsoft.NET\Framework\v4.0.30319"
    "%SystemRoot%\Microsoft.NET\Framework64\v3.5"
    "%SystemRoot%\Microsoft.NET\Framework\v3.5"
) do (
    if exist "%%~d\csc.exe" (
        if "%CSC%"=="" set "CSC=%%~d\csc.exe"
    )
)

if "%CSC%"=="" (
    echo [错误] 未找到 .NET Framework C# 编译器（csc.exe）
    echo Windows 7 及以上系统应默认包含。请确保 .NET Framework 4.0 已安装。
    pause & exit /b 1
)

echo 使用编译器: %CSC%
echo.

set "OUT=%~dp0..\portable"
set "SRC=%~dp0"

:: ── 编译启动器 ─────────────────────────────────────
echo [1/2] 编译 启动系统.exe ...
"%CSC%" /nologo /utf8output /target:exe ^
    /out:"%OUT%\启动系统.exe" ^
    "%SRC%Launcher.cs"
if %errorlevel% neq 0 (
    echo [错误] 编译 Launcher.cs 失败
    pause & exit /b 1
)
echo       OK: portable\启动系统.exe

:: ── 编译停止器 ─────────────────────────────────────
echo [2/2] 编译 停止系统.exe ...
"%CSC%" /nologo /utf8output /target:exe ^
    /out:"%OUT%\停止系统.exe" ^
    "%SRC%Stopper.cs"
if %errorlevel% neq 0 (
    echo [错误] 编译 Stopper.cs 失败
    pause & exit /b 1
)
echo       OK: portable\停止系统.exe

echo.
echo 编译完成！两个 exe 已输出到 portable\ 目录。
echo.
