#!/usr/bin/env sh
# Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
# Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations.
set -eu
cd "$(dirname "$0")"
mkdir -p out
java -m jdk.compiler/com.sun.tools.javac.Main -d out src/*.java tests/*.java
java -cp out CandleTests
java -cp out ChatTests
