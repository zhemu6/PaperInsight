#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script (POSIX)
#
# This script is part of Maven Wrapper and will download and run Maven.
# ----------------------------------------------------------------------------

set -euf

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_LAUNCHER="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_LAUNCHER" ]; then
  echo "Missing $WRAPPER_LAUNCHER" >&2
  exit 1
fi

JAVA_CMD=${JAVA_HOME:+"$JAVA_HOME/bin/java"}
JAVA_CMD=${JAVA_CMD:-java}

exec "$JAVA_CMD" -classpath "$WRAPPER_LAUNCHER" -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -Dmaven.wrapper.properties="$PROPS" org.apache.maven.wrapper.MavenWrapperMain "$@"
