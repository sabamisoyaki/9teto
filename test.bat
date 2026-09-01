@echo off
setlocal enabledelayedexpansion
rem powershell.exe lives in System32\WindowsPowerShell\v1.0\, not System32 itself
set "PATH=%SystemRoot%\System32;%SystemRoot%\System32\WindowsPowerShell\v1.0;%PATH%"
cd /d "%~dp0"

echo ----------------------------------------
echo  Running tests
echo ----------------------------------------

rem ---- Find JDK ----
set "JAVA_BIN="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVA_BIN=%JAVA_HOME%\bin"
)
if not defined JAVA_BIN (
    for /f "delims=" %%J in ('where javac 2^>nul') do (
        set "JAVA_BIN=%%~dpJ"
        goto :foundJavac
    )
)
if not defined JAVA_BIN (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do (
        if exist "%%~fD\bin\javac.exe" ( set "JAVA_BIN=%%~fD\bin" & goto :foundJavac )
    )
)
:foundJavac
if not defined JAVA_BIN (
    echo [ERROR] javac.exe not found. Install a JDK or set JAVA_HOME.
    exit /b 1
)

set "FX=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
if not exist "%FX%" (
    echo [ERROR] JavaFX SDK not found at: %FX%
    exit /b 1
)

rem Tests never open a window, but they touch types that reference JavaFX
rem (KeyCode, Color), so the module path is needed to compile and run.
set "OUT=%~dp0out-test"
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo [1/2] Compiling src and test...
dir /s /B src\*.java test\*.java > test-sources.txt
"%JAVA_BIN%\javac.exe" -d "%OUT%" -encoding UTF-8 ^
    --module-path "%FX%" --add-modules javafx.controls,javafx.media,javafx.graphics ^
    @test-sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    del test-sources.txt
    exit /b 1
)
del test-sources.txt

echo [2/2] Running AllTests...
echo.
"%JAVA_BIN%\java.exe" -Dfile.encoding=UTF-8 ^
    --module-path "%FX%" --add-modules javafx.controls,javafx.media,javafx.graphics ^
    -cp "%OUT%" tetris.AllTests
set "RESULT=%ERRORLEVEL%"

echo.
if "%RESULT%"=="0" (
    echo ========================================
    echo  ALL TESTS PASSED
    echo ========================================
) else (
    echo ========================================
    echo  TESTS FAILED
    echo ========================================
)
exit /b %RESULT%
