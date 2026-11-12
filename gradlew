#!/bin/sh
# FIRE GUN - Gradle wrapper launcher.
# If gradle-wrapper.jar is missing, it is generated with a locally installed `gradle`.
APP_HOME=$(cd "$(dirname "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  if command -v gradle >/dev/null 2>&1; then
    (cd "$APP_HOME" && gradle wrapper --gradle-version 8.7) || exit 1
  else
    echo "gradle-wrapper.jar not found and 'gradle' is not installed."
    echo "Install Gradle 8.7 or run the GitHub Actions workflow."
    exit 1
  fi
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
