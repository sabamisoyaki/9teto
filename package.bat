@echo off
setlocal enabledelayedexpansion
rem powershell.exe は System32 直下ではなく System32\WindowsPowerShell\v1.0\ にある。
rem System32 だけ通しても見つからないので、両方を先頭へ足す
set "PATH=%SystemRoot%\System32;%SystemRoot%\System32\WindowsPowerShell\v1.0;%PATH%"
cd /d "%~dp0"

echo ========================================
echo  Packaging TETRIS as standalone exe
echo ========================================

rem ---- Find JDK ----
set "JDK="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
)
if not defined JDK (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do (
        if exist "%%~fD\bin\jpackage.exe" ( set "JDK=%%~fD" & goto :foundJdk )
    )
)
:foundJdk
if not defined JDK (
    echo [ERROR] JDK with jpackage.exe not found.
    echo         Install JDK 14+ or set JAVA_HOME.
    pause & goto :eof
)
echo JDK : %JDK%

set "JAVAC=%JDK%\bin\javac.exe"
set "JAR=%JDK%\bin\jar.exe"
set "JPACKAGE=%JDK%\bin\jpackage.exe"
set "FX_LIB=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
set "FX_BIN=%~dp0openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\bin"

if not exist "%FX_LIB%" (
    echo [ERROR] JavaFX SDK not found at: %FX_LIB%
    pause & goto :eof
)

rem ---- Step 1: Compile ----
echo.
echo [1/5] Compiling...
if exist "out" rmdir /s /q "out"
mkdir "out"
dir /s /B src\*.java > sources.txt
"%JAVAC%" -d out -encoding UTF-8 --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.graphics @sources.txt
if errorlevel 1 ( del sources.txt & echo [ERROR] Compilation failed. & pause & goto :eof )
del sources.txt

rem ---- Step 2: Build jar (Launcher as main class) ----
echo [2/5] Building app.jar...
cd out
"%JAR%" --create --file ..\app.jar --main-class tetris.Launcher -C . .
if errorlevel 1 ( cd .. & echo [ERROR] jar creation failed. & pause & goto :eof )
cd ..

rem ---- Step 3: Prepare package-input directory ----
echo [3/5] Preparing package-input...
if exist "package-input" rmdir /s /q "package-input"
mkdir "package-input"
mkdir "package-input\javafx-libs"

copy "app.jar"           "package-input\app.jar"         >nul
xcopy "%FX_LIB%\*.jar"  "package-input\javafx-libs\" /Y /Q >nul
xcopy "%FX_BIN%\*.dll"  "package-input\"              /Y /Q >nul

if exist "images" xcopy "images" "package-input\images\" /E /I /Y /Q >nul
if exist "audio"  xcopy "audio"  "package-input\audio\"  /E /I /Y /Q >nul

rem Adventure part scenario. Without this the packaged build silently skips the parts.
if exist "scenario" xcopy "scenario" "package-input\scenario\" /E /I /Y /Q >nul

rem images\archive holds unreferenced art kept for reference only. Do not ship it.
if exist "package-input\images\archive" rmdir /s /q "package-input\images\archive"

rem ---- Step 4: jpackage ----
echo [4/5] Running jpackage...
if exist "dist" rmdir /s /q "dist"
mkdir "dist"

"%JPACKAGE%" ^
  --type app-image ^
  --name Tetris ^
  --app-version 1.0.0 ^
  --input "package-input" ^
  --main-jar app.jar ^
  --main-class tetris.Launcher ^
  --runtime-image "%JDK%" ^
  --java-options "--module-path $APPDIR/javafx-libs" ^
  --java-options "--add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.media" ^
  --java-options "--enable-native-access=javafx.graphics,javafx.media" ^
  --java-options "-Djava.library.path=$APPDIR" ^
  --dest dist

if errorlevel 1 ( echo [ERROR] jpackage failed. & pause & goto :eof )

rem ---- Step 5: Cleanup ----
echo [5/5] Cleaning up...
rmdir /s /q "package-input"

rem ---- Step 6: Zip ----
echo [6/6] Creating zip archive...
if exist "dist\Tetris.zip" del /f /q "dist\Tetris.zip"

rem PATH に頼らず絶対パスで叩く。PATH の中身は環境によって削られていることがある
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if exist "%PS%" (
    "%PS%" -NoProfile -Command "try { Compress-Archive -Path 'dist\Tetris' -DestinationPath 'dist\Tetris.zip' -ErrorAction Stop } catch { exit 1 }"
) else (
    rem PowerShell が無い環境の逃げ道。tar は Windows 10 1803 以降に同梱されている
    tar -c -f "dist\Tetris.zip" --format=zip -C dist Tetris
)

rem 終了コードだけだと取りこぼすことがあるので、実物ができたかで判定する
if not exist "dist\Tetris.zip" ( echo [ERROR] Zip creation failed. & pause & goto :eof )

echo.
echo ==========================================
echo  DONE!
echo  Executable : dist\Tetris\Tetris.exe
echo  Zip        : dist\Tetris.zip
echo ==========================================
pause
