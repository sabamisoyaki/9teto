@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ----------------------------------------
echo  Building TETRIS (Compiling Java files)
echo ----------------------------------------

rem Resolve javac and jar
set "JAVA_BIN="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVA_BIN=%JAVA_HOME%\bin"
    )
)

if not defined JAVA_BIN (
    for /f "delims=" %%J in ('where javac 2^>nul') do (
        set "JAVA_BIN=%%~dpJ"
        goto :foundJavac
    )
)

if not defined JAVA_BIN (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do (
        if exist "%%~fD\bin\javac.exe" (
            set "JAVA_BIN=%%~fD\bin"
            goto :foundJavac
        )
    )
)

:foundJavac
if not defined JAVA_BIN (
    echo [ERROR] javac.exe not found. Please install JDK or set JAVA_HOME.
    pause
    goto :eof
)

set "FX=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
if not exist "%FX%" (
    echo [ERROR] JavaFX SDK not found at: %FX%
    pause
    goto :eof
)

echo [1/3] Cleaning old build...
if exist "out" rmdir /s /q "out"
mkdir "out"

echo [2/3] Compiling source files...
dir /s /B src\*.java > sources.txt
"%JAVA_BIN%\javac.exe" -d out -encoding UTF-8 --module-path "%FX%" --add-modules javafx.controls,javafx.media,javafx.graphics @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt
    pause
    goto :eof
)
del sources.txt

echo [3/3] Creating app.jar...
cd out
"%JAVA_BIN%\jar.exe" --create --file ..\app.jar --main-class tetris.Launcher -C . .
if errorlevel 1 (
    echo [ERROR] Failed to create app.jar!
    cd ..
    pause
    goto :eof
)
cd ..

echo.
echo ========================================
echo  BUILD SUCCESSFUL! app.jar was updated.
echo ========================================
pause
