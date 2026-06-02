@echo off
echo Starting BJTU Canteen System...
docker compose up -d --build
if %errorlevel% neq 0 (
    echo Failed. Please make sure Docker Desktop is running.
    pause
    exit /b 1
)
echo.
echo Done! Open browser: http://localhost
echo.
pause
