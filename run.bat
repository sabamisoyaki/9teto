@echo off
setlocal
cd /d "%~dp0"

rem Resolve Java executable
set "JAVA="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA=%JAVA_HOME%\bin\java.exe"
    )
)

if not defined JAVA (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        set "JAVA=%%J"
        goto :foundJava
    )
)

if not defined JAVA (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do (
        if exist "%%~fD\bin\java.exe" (
            set "JAVA=%%~fD\bin\java.exe"
            goto :foundJava
        )
    )
)

:foundJava
if not defined JAVA (
    echo Java not found. Set JAVA_HOME or ensure java.exe is on PATH.
    goto :eof
)

set "FX=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
if not exist "%FX%" (
    echo JavaFX SDK not found at: %FX%
    echo Please download and unzip it to the expected path.
    goto :eof
)

"%JAVA%" --module-path "%FX%" ^
       --enable-native-access=javafx.graphics,javafx.media ^
       --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.media ^
       -jar app.jar

if errorlevel 1 (
    echo.
    echo ==========================
    echo  Execution failed.
    echo ==========================
    pause >nul
    goto :eof
)

echo.
echo ==========================
echo  Press any key to exit...
echo ==========================
pause >nul
