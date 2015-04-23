#!/usr/bin/env bash

JAVA_VER=$(java -version 2>&1 | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
JAVA=java

if [ "$JAVA_VER" -ge 18 ]; then
   JAVA=java
else
   JAVA=/local/jdk1.8.0_31/bin/java
fi

$JAVA -cp src/main/java Main $1 $2
