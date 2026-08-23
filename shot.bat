@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ----------------------------------------------------------------
rem  UI check: shoot every screen into docs\shots as PNG.
rem  The app draws each screen itself and calls Scene.snapshot(),
rem  so no window focus / screen coordinates are involved.
rem  What gets shot is defined in src\tetris\ShotRunner.java.
rem
rem    shot.bat            build, then shoot
rem    shot.bat -nobuild   shoot with the current app.jar
rem    shot.bat -keep      also copy the result to docs\shots-<timestamp>
rem ----------------------------------------------------------------

set "DO_BUILD=1"
set "DO_KEEP="

:parseArgs
if "%~1"=="" goto :argsDone
if /i "%~1"=="-nobuild" set "DO_BUILD="
if /i "%~1"=="-keep"    set "DO_KEEP=1"
shift
goto :parseArgs
:argsDone

set "OUT=%~dp0docs\shots"

rem Resolve Java executable (same lookup as run.bat)
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
    echo [ERROR] Java not found. Set JAVA_HOME or ensure java.exe is on PATH.
    pause
    goto :eof
)

set "FX=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
if not exist "%FX%" (
    echo [ERROR] JavaFX SDK not found at: %FX%
    pause
    goto :eof
)

if defined DO_BUILD (
    call "%~dp0build.bat" <nul
    rem build.bat always exits 0, so check that the class actually landed
    if not exist "%~dp0out\tetris\ShotRunner.class" (
        echo.
        echo [ERROR] Build failed. See the build output above.
        pause
        goto :eof
    )
)

if not exist "%~dp0app.jar" (
    echo [ERROR] app.jar not found. Run shot.bat without -nobuild.
    pause
    goto :eof
)

if not exist "%~dp0log" mkdir "%~dp0log"
if not exist "%OUT%" mkdir "%OUT%"

rem Wipe old PNGs so a removed layout does not leave a stale file behind
if exist "%OUT%\*.png" del /q "%OUT%\*.png"

echo.
echo ----------------------------------------
echo  Shooting UI screens
echo  out: %OUT%
echo ----------------------------------------

rem -Dglass.win.uiScale=1.0 keeps the window at logical 1:1 on a scaled
rem desktop, so the snapshots come out exactly 1920x1080.
"%JAVA%" --module-path "%FX%" ^
       -Dglass.win.uiScale=1.0 ^
       -Dshot.out="%OUT%" ^
       --enable-native-access=javafx.graphics,javafx.media ^
       --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.media ^
       "-XX:ErrorFile=%~dp0log\hs_err_pid%%p.log" ^
       -jar app.jar

if errorlevel 1 (
    echo.
    echo [ERROR] Shooting failed.
    pause
    goto :eof
)

if defined DO_KEEP (
    rem Call powershell by full path: it is not always on PATH, and an empty
    rem stamp would silently create a folder literally named "shots-".
    set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
    set "STAMP="
    if exist "!PS!" (
        for /f "delims=" %%T in ('!PS! -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "STAMP=%%T"
    )
    if not defined STAMP set "STAMP=latest"
    set "KEEPDIR=%~dp0docs\shots-!STAMP!"
    if not exist "!KEEPDIR!" mkdir "!KEEPDIR!"
    copy /y "%OUT%\*.png" "!KEEPDIR!" >nul
    echo  kept a copy in: !KEEPDIR!
)

start "" "%OUT%"

echo.
echo ========================================
echo  DONE. Press any key to exit...
echo ========================================
pause >nul
