@echo off
setlocal
REM Build BUDDY AI MIRA after installing Java 17, Android SDK 35 and Gradle 8.9.
where gradle >nul 2>&1
if errorlevel 1 (
  echo Gradle was not found on PATH. Use Android Studio or GitHub Actions instead.
  pause
  exit /b 1
)
gradle --no-daemon :app:assembleDebug
if errorlevel 1 (
  echo Build failed. Review Gradle output.
  pause
  exit /b 1
)
echo APK created at app\build\outputs\apk\debug\app-debug.apk
pause
