@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
echo Done. You can close this window.
pause
