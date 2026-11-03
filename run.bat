@echo off
setlocal
cd /d "%~dp0"

call mvn -q compile
if errorlevel 1 exit /b %errorlevel%

call mvn -q dependency:build-classpath "-Dmdep.includeScope=compile" "-Dmdep.outputFile=target\javafx-classpath.txt"
if errorlevel 1 exit /b %errorlevel%

set /p JAVAFX_MODULE_PATH=<target\javafx-classpath.txt
java --module-path "%JAVAFX_MODULE_PATH%" --add-modules javafx.controls -classpath "target\classes" campsite.ui.CampsiteApp
