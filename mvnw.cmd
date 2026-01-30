@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script (Windows)
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set BASE_DIR=%~dp0
set WRAPPER_LAUNCHER=%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar
set PROPS=%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_LAUNCHER%" (
  echo Missing %WRAPPER_LAUNCHER%
  exit /b 1
)

if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)

"%JAVA_CMD%" -classpath "%WRAPPER_LAUNCHER%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -Dmaven.wrapper.properties="%PROPS%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
