9pazzle Tetris
==============

How to run (development)
--------------------------
Prerequisites:
  - JDK 25+ (Eclipse Adoptium recommended)
  - JavaFX SDK 25.0.1 extracted to:
      openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1

Steps:
  1. Double-click build.bat to compile and create app.jar
  2. Double-click run.bat (or run.vbs for silent launch) to start the game

How to build a standalone exe
------------------------------
Run package.bat — no arguments needed.

  Output: dist\Tetris\Tetris.exe

The dist\Tetris\ folder contains a bundled JRE and all assets.
Share the entire folder; no Java installation required on the target machine.

BGM (optional)
---------------
Place audio\bgm.wav in the project root.
The game loops it during gameplay. Runs silently if the file is missing.
