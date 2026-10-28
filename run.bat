@echo off
cd /d "%~dp0"
if not exist out mkdir out
java -m jdk.compiler/com.sun.tools.javac.Main -d out src\CandleCalculator.java src\MarketData.java src\CandleServer.java
if errorlevel 1 exit /b 1
java -cp out CandleServer
