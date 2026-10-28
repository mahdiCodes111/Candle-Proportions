#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
java -m jdk.compiler/com.sun.tools.javac.Main -d out src/*.java tests/*.java
java -cp out CandleTests
