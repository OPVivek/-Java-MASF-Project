# PowerShell script to compile, verify, and run MASF Enterprise Task Engine

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   MASF Enterprise Task Engine - PowerShell Runner" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$javacBin = "javac"
$javaBin = "java"

if (Test-Path "C:\Program Files\Java\jdk-17\bin\javac.exe") {
    $javacBin = "C:\Program Files\Java\jdk-17\bin\javac.exe"
    $javaBin = "C:\Program Files\Java\jdk-17\bin\java.exe"
}

Write-Host "`n[1/4] Compiling Project..." -ForegroundColor Yellow
& $javacBin -d . interfaces/*.java utils/*.java core/*.java tests/*.java Main.java

Write-Host "`n[2/4] Executing Core Verification Test Suite..." -ForegroundColor Yellow
& $javaBin tests.MASFTestSuite

Write-Host "`n[3/4] Executing Input Variations & Boundary Stress Test Suite..." -ForegroundColor Yellow
& $javaBin tests.MASFVariationsTest

Write-Host "`n[4/4] Launching MASF Automated Benchmark Demo..." -ForegroundColor Yellow
& $javaBin Main --demo

Write-Host "`n[+] Complete Verification and Execution Succeeded!" -ForegroundColor Green
