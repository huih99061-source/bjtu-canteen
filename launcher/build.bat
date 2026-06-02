@echo off
setlocal
echo Building launcher exes via PowerShell...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
exit /b %errorlevel%
