@echo off
chcp 65001 > nul
setlocal

set "DIR=%~dp0"
set "MYSQL_DIR=%DIR%mysql"
set "MYSQL_BIN=%MYSQL_DIR%\bin"
set "MYSQL_DATA=%MYSQL_DIR%\data"
set "MYSQL_PORT=3307"
set "JRE_BIN=%DIR%jre\bin"
set "JAR=%DIR%app.jar"

echo ==================================================
echo   北京交通大学食堂仿真系统 v1.0  [便携版]
echo ==================================================
echo.

:: ── 检查依赖 ──────────────────────────────────────
if not exist "%JRE_BIN%\java.exe" (
    echo [错误] 未找到 jre\bin\java.exe
    echo 请先运行"构建便携包.bat"完成打包。
    pause & exit /b 1
)
if not exist "%MYSQL_BIN%\mysqld.exe" (
    echo [错误] 未找到 mysql\bin\mysqld.exe
    echo 请先运行"构建便携包.bat"完成打包。
    pause & exit /b 1
)

:: ── 首次运行：初始化数据库 ─────────────────────────
if not exist "%MYSQL_DATA%\" (
    echo [初始化] 首次运行，正在初始化数据库，请稍候...
    "%MYSQL_BIN%\mysqld.exe" ^
        --defaults-file="%MYSQL_DIR%\my.ini" ^
        --basedir="%MYSQL_DIR%" ^
        --datadir="%MYSQL_DATA%" ^
        --initialize-insecure ^
        --console 2>nul
    echo [初始化] 启动数据库服务...
    start "BJTU-MySQL" /b "%MYSQL_BIN%\mysqld.exe" ^
        --defaults-file="%MYSQL_DIR%\my.ini" ^
        --basedir="%MYSQL_DIR%" ^
        --datadir="%MYSQL_DATA%"
    echo [初始化] 等待数据库就绪（约10秒）...
    timeout /t 10 /nobreak > nul
    echo [初始化] 导入初始数据...
    "%MYSQL_BIN%\mysql.exe" -u root -P%MYSQL_PORT% --host=127.0.0.1 --protocol=TCP < "%DIR%init.sql"
    if %errorlevel% neq 0 (
        echo [错误] 数据导入失败，请重试或检查 mysql 目录。
        pause & exit /b 1
    )
    echo [初始化] 数据库初始化完成！
    echo.
) else (
    echo [1/2] 启动数据库服务...
    start "BJTU-MySQL" /b "%MYSQL_BIN%\mysqld.exe" ^
        --defaults-file="%MYSQL_DIR%\my.ini" ^
        --basedir="%MYSQL_DIR%" ^
        --datadir="%MYSQL_DATA%"
    timeout /t 4 /nobreak > nul
    echo [2/2] 数据库就绪。
    echo.
)

:: ── 启动后端 ──────────────────────────────────────
echo [启动] 启动后端服务（首次约需30秒）...
set "DB_URL=jdbc:mysql://127.0.0.1:%MYSQL_PORT%/bjtu_canteen?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true"
start "BJTU-Backend" /b "%JRE_BIN%\java.exe" -jar "%JAR%" ^
    "--spring.datasource.url=%DB_URL%" ^
    --spring.datasource.username=root ^
    "--spring.datasource.password="

echo [启动] 等待后端就绪（约15秒）...
timeout /t 15 /nobreak > nul

:: ── 打开浏览器 ────────────────────────────────────
echo [完成] 正在打开浏览器...
start "" "%DIR%frontend\index.html"
echo.
echo ✅ 系统已启动！
echo    若浏览器未自动打开，请手动双击 frontend\index.html
echo.
echo 关闭系统请双击"停止.bat"
echo 按任意键关闭此窗口（系统继续在后台运行）
pause > nul
