@echo off
setlocal
cd /d "%~dp0"
set "MAVEN=mvn"
where mvn >nul 2>nul
if errorlevel 1 set "MAVEN=C:\Tools\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd"
call "%MAVEN%" clean package
if errorlevel 1 (
 echo Build failed. Read the error above.
 pause
 exit /b 1
)
echo Build completed. Start with start.cmd.
pause
