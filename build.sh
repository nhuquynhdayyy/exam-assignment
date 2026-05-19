#!/bin/bash
echo "========================================="
echo " BUILD: He thong Phan Cong Can Bo Coi Thi"
echo " Version 2.0 - Java (MySQL + Multi-Ca)"
echo "========================================="

mvn clean package -q

if [ $? -eq 0 ]; then
    echo ""
    echo "[OK] Build thanh cong!"
    echo "  - target/ExamServer.jar  (GUI Server + MySQL)"
    echo "  - target/ExamClient.jar  (GUI Client)"
    echo ""
    echo "Chay Server: java -jar target/ExamServer.jar"
    echo "Chay Client: java -jar target/ExamClient.jar"
else
    echo "[LOI] Build that bai!"
    exit 1
fi
