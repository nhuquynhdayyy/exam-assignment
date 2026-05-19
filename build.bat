@echo off
chcp 65001 > nul
echo =========================================
echo  BUILD: He thong Phan Cong Can Bo Coi Thi
echo  Version 2.0 - Java (MySQL + Multi-Ca)
echo =========================================

mvn clean package -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] Build thanh cong!
    echo  - ExamServer.jar  (GUI Server + MySQL)
    echo  - ExamClient.jar  (GUI Client)
    echo.
    echo Chay Server: java -jar target\ExamServer.jar
    echo Chay Client: java -jar target\ExamClient.jar
) else (
    echo [LOI] Build that bai! Xem log o tren.
)
pause
