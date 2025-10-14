@echo off
set CURRENT_DIR=%~dp0
set JAVA_HOME=%CURRENT_DIR%jdk
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
set APP_NAME=ExternelDataManagement
set JAR_FILE=ExternelDataManagement.jar
set CONFIG_FILE=application.properties

echo Starting %APP_NAME% Application...
java %JAVA_OPTS% -jar "%CURRENT_DIR%%JAR_FILE%" --spring.config.location=file:./%CONFIG_FILE%

if %ERRORLEVEL% NEQ 0 (
    echo Application failed to start with error code: %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

pause
