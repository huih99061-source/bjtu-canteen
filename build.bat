@echo off
setlocal

set "ROOT=%~dp0"
set "PORTABLE=%ROOT%portable"

echo [Check] JRE...
if not exist "%PORTABLE%\jre\bin\java.exe" (
    echo ERROR: portable\jre\bin\java.exe not found.
    echo Please extract JRE 21 into portable\jre\
    pause & exit /b 1
)

echo [Check] MySQL...
if not exist "%PORTABLE%\mysql\bin\mysqld.exe" (
    echo ERROR: portable\mysql\bin\mysqld.exe not found.
    echo Please extract MySQL into portable\mysql\
    pause & exit /b 1
)

echo [Check] Maven...
where mvn > nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: mvn not found. Please install Maven and add to PATH.
    pause & exit /b 1
)

echo [1/5] Building launcher exe...
call "%ROOT%launcher\build.bat"
if %errorlevel% neq 0 (
    echo ERROR: Launcher build failed.
    pause & exit /b 1
)

echo [2/5] Building backend...
cd "%ROOT%backend"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed.
    pause & exit /b 1
)
cd "%ROOT%"
echo Done.

echo [3/5] Copying files...
copy "%ROOT%backend\target\canteen-sim-1.0.0.jar" "%PORTABLE%\app.jar" > nul
if exist "%PORTABLE%\frontend\" rd /s /q "%PORTABLE%\frontend"
xcopy "%ROOT%frontend" "%PORTABLE%\frontend" /E /I /Y /Q > nul
copy "%ROOT%database\init.sql" "%PORTABLE%\init.sql" > nul
echo Done.

echo [4/5] Cleaning up...
if exist "%PORTABLE%\canteen.log" del "%PORTABLE%\canteen.log"
if exist "%PORTABLE%\mysql\data\" rd /s /q "%PORTABLE%\mysql\data"
echo Done.

echo [5/5] Packaging ZIP...
set "ZIPFILE=%ROOT%bjtu-canteen-portable.zip"
if exist "%ZIPFILE%" del "%ZIPFILE%"
powershell -NoProfile -Command "Compress-Archive -Path '%PORTABLE%\*' -DestinationPath '%ZIPFILE%' -Force"
if %errorlevel% neq 0 (
    echo ERROR: ZIP packaging failed.
    pause & exit /b 1
)
echo Done: bjtu-canteen-portable.zip

echo.
echo Build complete! Upload bjtu-canteen-portable.zip to GitHub Releases.
echo.
pause
