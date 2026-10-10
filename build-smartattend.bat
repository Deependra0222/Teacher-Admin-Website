@echo off
setlocal
set "SMARTATTEND_JDK=%~dp0.toolchains\jdk17\jdk-17.0.20.1+1"
if not exist "%SMARTATTEND_JDK%\bin\java.exe" (
  echo SmartAttend needs JDK 17. In Android Studio, set Gradle JDK to a JDK 17 installation.
  exit /b 1
)
set "JAVA_HOME=%SMARTATTEND_JDK%"
call "%~dp0gradlew.bat" %*

