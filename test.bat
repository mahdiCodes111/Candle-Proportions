@echo off
REM Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
REM Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations.
cd /d "%~dp0"
if not exist out mkdir out
java -m jdk.compiler/com.sun.tools.javac.Main -d out src\CandleCalculator.java src\MarketData.java src\CandleServer.java tests\CandleTests.java
if errorlevel 1 exit /b 1
java -cp out CandleTests
