@echo off
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set DB_NAME=college_recommendation

if not "%~1"=="" set MYSQL_HOST=%~1
if not "%~2"=="" set MYSQL_PORT=%~2
if not "%~3"=="" set MYSQL_USER=%~3
if not "%~4"=="" set MYSQL_PASSWORD=%~4
if not "%~5"=="" set DB_NAME=%~5

where mysql >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mysql command not found. Please add MySQL bin directory to PATH.
  echo Example: C:\Program Files\MySQL\MySQL Server 8.0\bin
  exit /b 1
)

echo [INFO] Initializing database...
echo [INFO] Host=%MYSQL_HOST% Port=%MYSQL_PORT% User=%MYSQL_USER% DB=%DB_NAME%

set MYSQL_AUTH=-p
if not "%MYSQL_PASSWORD%"=="" set MYSQL_AUTH=--password=%MYSQL_PASSWORD%

mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% %MYSQL_AUTH% --default-character-set=utf8mb4 < "%SCRIPT_DIR%sql\schema.sql"
if errorlevel 1 (
  echo [ERROR] Failed to execute schema.sql
  exit /b 1
)

mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% %MYSQL_AUTH% --default-character-set=utf8mb4 < "%SCRIPT_DIR%sql\data.sql"
if errorlevel 1 (
  echo [ERROR] Failed to execute data.sql
  exit /b 1
)

echo [OK] Database initialization completed.
echo [INFO] You can now run the backend application.
exit /b 0
