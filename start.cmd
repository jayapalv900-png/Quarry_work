@echo off
setlocal
cd /d "%~dp0"
if not exist "target\quarryworks-2.0.0.jar" (
 echo Build the project with build.cmd first.
 pause
 exit /b 1
)
java -jar "target\quarryworks-2.0.0.jar"
pause
