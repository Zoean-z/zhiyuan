@echo off
setlocal

set MYSQL_HOST=%DB_HOST%
if "%MYSQL_HOST%"=="" set MYSQL_HOST=localhost

set MYSQL_PORT=%DB_PORT%
if "%MYSQL_PORT%"=="" set MYSQL_PORT=3307

set MYSQL_USER=%DB_USER%
if "%MYSQL_USER%"=="" set MYSQL_USER=zhiyuan

set MYSQL_PASSWORD=%DB_PASSWORD%
if "%MYSQL_PASSWORD%"=="" set MYSQL_PASSWORD=zhiyuan123

if not "%~1"=="" set MYSQL_HOST=%~1
if not "%~2"=="" set MYSQL_PORT=%~2
if not "%~3"=="" set MYSQL_USER=%~3
if not "%~4"=="" set MYSQL_PASSWORD=%~4

if "%MYSQL_PASSWORD%"=="" (
  echo [ERROR] DB password is empty.
  echo [HINT] Set DB_PASSWORD env var or pass the 4th arg:
  echo        check-db.bat localhost 3306 root your_password
  exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mysql command not found. Please add MySQL bin directory to PATH.
  echo [HINT] Example: C:\Program Files\MySQL\MySQL Server 8.0\bin
  exit /b 1
)

mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% --password=%MYSQL_PASSWORD% --connect-timeout=5 -e "SELECT 1;" >nul 2>nul
if errorlevel 1 (
  echo [FAIL] Database connection failed.
  echo [INFO] Host=%MYSQL_HOST% Port=%MYSQL_PORT% User=%MYSQL_USER%
  exit /b 1
)

echo [OK] Database connection passed.
echo [INFO] Host=%MYSQL_HOST% Port=%MYSQL_PORT% User=%MYSQL_USER%
exit /b 0
