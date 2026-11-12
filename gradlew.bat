@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  where gradle >nul 2>nul
  if errorlevel 1 (
    echo gradle-wrapper.jar not found and gradle is not installed.
    exit /b 1
  )
  pushd "%APP_HOME%"
  gradle wrapper --gradle-version 8.7
  popd
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
