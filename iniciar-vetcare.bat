@echo off
title VetCare Pro - Sistema Veterinario
cd /d "%~dp0"

echo ==================================================
echo   Iniciando VetCare Pro (Spring Boot + Front-end)
echo ==================================================
echo.

if not exist "target\veterinaria-1.0.0.jar" (
    echo Empaquetando aplicacion por primera vez...
    call mvnw.cmd clean package -DskipTests
    if errorlevel 1 (
        echo Error al compilar el proyecto.
        pause
        exit /b 1
    )
)

echo Iniciando servidor en http://localhost:8080 ...
start "" cmd /c "timeout /t 6 /nobreak >nul & start http://localhost:8080"
java -jar target\veterinaria-1.0.0.jar
pause
