@echo off
chcp 65001 > nul
echo ==================================================
echo  便携包构建脚本（开发者使用）
echo ==================================================
echo.

set "ROOT=%~dp0"
set "PORTABLE=%ROOT%portable"

:: 检查 JRE
if not exist "%PORTABLE%\jre\bin\java.exe" (
    echo [前置条件] 未找到 portable\jre\bin\java.exe
    echo.
    echo 请手动完成以下步骤：
    echo  1. 打开 https://adoptium.net/zh-CN/temurin/releases/
    echo     选择：Version=21, OS=Windows, Arch=x64, Package=JRE
    echo  2. 下载 .zip 格式
    echo  3. 解压，将内容放入 %PORTABLE%\jre\
    echo     （确保 portable\jre\bin\java.exe 存在）
    echo.
    pause & exit /b 1
)

:: 检查 MySQL
if not exist "%PORTABLE%\mysql\bin\mysqld.exe" (
    echo [前置条件] 未找到 portable\mysql\bin\mysqld.exe
    echo.
    echo 请手动完成以下步骤：
    echo  1. 打开 https://dev.mysql.com/downloads/mysql/
    echo     选择 MySQL 8.0 或以上, Windows, ZIP Archive
    echo  2. 解压，将内容放入 %PORTABLE%\mysql\
    echo     （确保 portable\mysql\bin\mysqld.exe 存在）
    echo.
    pause & exit /b 1
)

:: 检查 Maven
where mvn > nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 mvn 命令，请先安装 Maven 并添加到 PATH。
    pause & exit /b 1
)

:: 编译 exe 启动器
echo [1/5] 编译 exe 启动器...
call "%ROOT%launcher\build.bat"
if %errorlevel% neq 0 (
    echo [错误] exe 编译失败，请检查 launcher\ 目录。
    pause & exit /b 1
)

:: 编译后端
echo [2/5] 正在编译后端...
cd "%ROOT%backend"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] Maven 编译失败，请检查代码。
    pause & exit /b 1
)
cd "%ROOT%"
echo       编译完成。

:: 复制运行时文件
echo [3/5] 复制文件到 portable\...
copy "%ROOT%backend\target\canteen-sim-1.0.0.jar" "%PORTABLE%\app.jar" > nul
if exist "%PORTABLE%\frontend\" rd /s /q "%PORTABLE%\frontend"
xcopy "%ROOT%frontend" "%PORTABLE%\frontend" /E /I /Y /Q > nul
copy "%ROOT%database\init.sql" "%PORTABLE%\init.sql" > nul
echo       复制完成。

:: 清理不应打包的文件
if exist "%PORTABLE%\启动.bat"  del "%PORTABLE%\启动.bat"
if exist "%PORTABLE%\停止.bat"  del "%PORTABLE%\停止.bat"
if exist "%PORTABLE%\canteen.log" del "%PORTABLE%\canteen.log"
if exist "%PORTABLE%\mysql\data\" rd /s /q "%PORTABLE%\mysql\data"
echo       已清理旧数据库数据。

:: 打包 ZIP
echo [4/5] 打包 ZIP...
set "ZIPFILE=%ROOT%bjtu-canteen-portable.zip"
if exist "%ZIPFILE%" del "%ZIPFILE%"
powershell -NoProfile -Command ^
  "Compress-Archive -Path '%PORTABLE%\*' -DestinationPath '%ZIPFILE%' -Force"
if %errorlevel% neq 0 (
    echo [警告] ZIP 打包失败，请手动压缩 portable\ 文件夹。
    pause & exit /b 1
)
echo       ZIP 已生成：bjtu-canteen-portable.zip

:: 完成
echo [5/5] 构建完成！
echo.
echo   输出文件：bjtu-canteen-portable.zip
echo   用户操作：解压 -^> 双击 食堂仿真系统.exe -^> 等待30秒 -^> 完成
echo.
pause
