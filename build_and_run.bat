@echo off
setlocal

echo ========================================================
echo   MASF Enterprise Task Engine - Build & Run Script
echo ========================================================

REM Look for JDK 17 in standard Program Files, or fall back to system PATH
if exist "C:\Program Files\Java\jdk-17\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-17\bin\javac.exe"
    set "JAVA_BIN=C:\Program Files\Java\jdk-17\bin\java.exe"
) else (
    set "JAVAC_BIN=javac"
    set "JAVA_BIN=java"
)

echo [1/4] Compiling source code and test suites...
"%JAVAC_BIN%" -d . interfaces/*.java utils/*.java core/*.java tests/*.java Main.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [2/4] Executing Core Verification test suite...
"%JAVA_BIN%" tests.MASFTestSuite
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Core tests failed!
    exit /b %ERRORLEVEL%
)

echo [3/4] Executing Input Variations and Boundary Stress test suite...
"%JAVA_BIN%" tests.MASFVariationsTest
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Variations tests failed!
    exit /b %ERRORLEVEL%
)

echo [4/4] Launching MASF Engine in Automated Demo Mode...
"%JAVA_BIN%" Main --demo

echo.
echo ========================================================
echo   Build and execution complete successfully!
echo ========================================================
pause
