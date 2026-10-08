@echo off
setlocal
set "JAVA_HOME=B:\Claude\Projekte\_RECOVERY\tools\jdk-21.0.12+8"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java 21 fehlt unter "%JAVA_HOME%".
    pause
    exit /b 1
)
cd /d "%~dp0"
call "%~dp0gradlew.bat" runClient --offline --console=plain
if errorlevel 1 (
    echo.
    echo Der Dev-Client konnte nicht gestartet werden. Bitte die Fehlermeldung oben pruefen.
    pause
    exit /b 1
)
endlocal
