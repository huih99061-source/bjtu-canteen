@echo off
chcp 65001 > nul
setlocal

set "DIR=%~dp0"
set "MYSQL_BIN=%DIR%mysql\bin"

echo 正在停止仿真系统...

:: 优雅关闭 MySQL
if exist "%MYSQL_BIN%\mysqladmin.exe" (
    "%MYSQL_BIN%\mysqladmin.exe" -u root -P3307 --host=127.0.0.1 --protocol=TCP shutdown 2>nul
)

:: 停止后端 Java 进程（按窗口标题）
taskkill /FI "WINDOWTITLE eq BJTU-Backend" /F /T > nul 2>&1

:: 兜底：直接结束进程（MySQL关闭失败时）
taskkill /F /IM mysqld.exe > nul 2>&1

echo ✅ 系统已停止。
pause
