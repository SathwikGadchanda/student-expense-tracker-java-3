@echo off
cd /d "%~dp0"
java -version >nul 2>&1
if errorlevel 1 (
  echo Java is missing. Install a Java 17 JDK and add its bin folder to PATH.
  pause
  exit /b 1
)
if not exist "target\student-expense-tracker.jar" (
  call mvn clean package
  if errorlevel 1 (
    echo Build failed. Check Java 17, Maven and the internet connection.
    pause
    exit /b 1
  )
)
java -jar "target\student-expense-tracker.jar" %*
if errorlevel 1 pause
