@echo off
REM ResourceFlow launcher for Windows.  Usage: run.bat            (desktop app)
REM                                            run.bat --console  (text menu)
title ResourceFlow
cd /d "%~dp0"

java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found on PATH. Please install Java 21 or later from https://adoptium.net/
    pause
    exit /b 1
)

set "JAR=target\resourceflow-1.0.0.jar"
if not exist "%JAR%" (
    echo JAR not found, building it first...
    call mvn.cmd -q package -DskipTests
    if errorlevel 1 (
        pause
        exit /b 1
    )
)

java -jar "%JAR%" %*
if errorlevel 1 (
    echo.
    echo ResourceFlow stopped with an error. It needs Java 21 or later - check with: java -version
    pause
)
