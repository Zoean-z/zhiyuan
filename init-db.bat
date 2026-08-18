@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MYSQL_HOST=%DB_HOST%"
if "%MYSQL_HOST%"=="" set "MYSQL_HOST=localhost"

set "MYSQL_PORT=%DB_PORT%"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3307"

set "MYSQL_USER=%DB_USER%"
if "%MYSQL_USER%"=="" set "MYSQL_USER=zhiyuan"

set "MYSQL_PASSWORD=%DB_PASSWORD%"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=zhiyuan123"

set "DB_NAME=%DB_NAME%"
if "%DB_NAME%"=="" set "DB_NAME=college_recommendation"

if not "%~1"=="" set "MYSQL_HOST=%~1"
if not "%~2"=="" set "MYSQL_PORT=%~2"
if not "%~3"=="" set "MYSQL_USER=%~3"
if not "%~4"=="" set "MYSQL_PASSWORD=%~4"
if not "%~5"=="" set "DB_NAME=%~5"

set "SCHEMA_FILE=%SCRIPT_DIR%sql\schema.sql"
set "DATA_FILE=%SCRIPT_DIR%sql\data.sql"
set "TEMP_SCHEMA_FILE="
set "TEMP_DATA_FILE="

if not exist "%SCHEMA_FILE%" (
  echo [ERROR] Missing file: %SCHEMA_FILE%
  exit /b 1
)

if not exist "%DATA_FILE%" (
  echo [ERROR] Missing file: %DATA_FILE%
  exit /b 1
)

set "MYSQL_CMD="
for %%I in (mysql.exe) do if not "%%~$PATH:I"=="" set "MYSQL_CMD=%%~$PATH:I"
if not defined MYSQL_CMD if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_CMD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if not defined MYSQL_CMD (
  echo [ERROR] mysql command not found. Please add MySQL bin directory to PATH.
  echo [HINT] Example: C:\Program Files\MySQL\MySQL Server 8.0\bin
  exit /b 1
)

if /I not "%DB_NAME%"=="college_recommendation" (
  call :prepare_sql_files
  if errorlevel 1 exit /b 1
)

echo [INFO] Initializing database...
echo [INFO] Host=%MYSQL_HOST% Port=%MYSQL_PORT% User=%MYSQL_USER% DB=%DB_NAME%

call :create_database
if errorlevel 1 (
  call :cleanup >nul 2>nul
  exit /b 1
)

call :run_sql "%SCHEMA_FILE%" "schema.sql"
if errorlevel 1 (
  call :cleanup >nul 2>nul
  exit /b 1
)

call :run_sql "%DATA_FILE%" "data.sql"
if errorlevel 1 (
  call :cleanup >nul 2>nul
  exit /b 1
)

call :cleanup >nul 2>nul
echo [OK] Database initialization completed.
echo [INFO] You can now run the backend application.
exit /b 0

:create_database
if "%MYSQL_PASSWORD%"=="" (
  "%MYSQL_CMD%" "--host=%MYSQL_HOST%" "--port=%MYSQL_PORT%" "--user=%MYSQL_USER%" --default-character-set=utf8mb4 --execute="CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
) else (
  "%MYSQL_CMD%" "--host=%MYSQL_HOST%" "--port=%MYSQL_PORT%" "--user=%MYSQL_USER%" "--password=%MYSQL_PASSWORD%" --default-character-set=utf8mb4 --execute="CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
)

if errorlevel 1 (
  echo [ERROR] Failed to create or verify database %DB_NAME%.
  exit /b 1
)

exit /b 0

:prepare_sql_files
set "TEMP_SCHEMA_FILE=%TEMP%\init-db-schema-%RANDOM%-%RANDOM%.sql"
set "TEMP_DATA_FILE=%TEMP%\init-db-data-%RANDOM%-%RANDOM%.sql"
set "INIT_DB_SOURCE_NAME=college_recommendation"
set "INIT_DB_TARGET_NAME=%DB_NAME%"
set "INIT_DB_SCHEMA_IN=%SCHEMA_FILE%"
set "INIT_DB_SCHEMA_OUT=%TEMP_SCHEMA_FILE%"
set "INIT_DB_DATA_IN=%DATA_FILE%"
set "INIT_DB_DATA_OUT=%TEMP_DATA_FILE%"

powershell -NoProfile -Command "$sourceName = [Environment]::GetEnvironmentVariable('INIT_DB_SOURCE_NAME'); $targetName = [Environment]::GetEnvironmentVariable('INIT_DB_TARGET_NAME'); $pairs = @(@{In=[Environment]::GetEnvironmentVariable('INIT_DB_SCHEMA_IN'); Out=[Environment]::GetEnvironmentVariable('INIT_DB_SCHEMA_OUT')}, @{In=[Environment]::GetEnvironmentVariable('INIT_DB_DATA_IN'); Out=[Environment]::GetEnvironmentVariable('INIT_DB_DATA_OUT')}); foreach ($pair in $pairs) { $content = Get-Content -Raw -Encoding UTF8 $pair.In; $content = $content.Replace($sourceName, $targetName); [System.IO.File]::WriteAllText($pair.Out, $content, [System.Text.UTF8Encoding]::new($false)) }"
if errorlevel 1 (
  echo [ERROR] Failed to prepare SQL files for database %DB_NAME%.
  call :cleanup >nul 2>nul
  exit /b 1
)

set "SCHEMA_FILE=%TEMP_SCHEMA_FILE%"
set "DATA_FILE=%TEMP_DATA_FILE%"
exit /b 0

:run_sql
set "CURRENT_SQL_FILE=%~1"
set "CURRENT_SQL_NAME=%~2"

if "%MYSQL_PASSWORD%"=="" (
  "%MYSQL_CMD%" "--host=%MYSQL_HOST%" "--port=%MYSQL_PORT%" "--user=%MYSQL_USER%" "--database=%DB_NAME%" --default-character-set=utf8mb4 < "%CURRENT_SQL_FILE%"
) else (
  "%MYSQL_CMD%" "--host=%MYSQL_HOST%" "--port=%MYSQL_PORT%" "--user=%MYSQL_USER%" "--password=%MYSQL_PASSWORD%" "--database=%DB_NAME%" --default-character-set=utf8mb4 < "%CURRENT_SQL_FILE%"
)

if errorlevel 1 (
  echo [ERROR] Failed to execute %CURRENT_SQL_NAME%
  exit /b 1
)

exit /b 0

:cleanup
if defined TEMP_SCHEMA_FILE if exist "%TEMP_SCHEMA_FILE%" del /q "%TEMP_SCHEMA_FILE%"
if defined TEMP_DATA_FILE if exist "%TEMP_DATA_FILE%" del /q "%TEMP_DATA_FILE%"
exit /b 0
