-- ============================================================
-- Script tạo database MySQL cho hệ thống phân công coi thi
-- Chạy: mysql -u root -p < setup_database.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS phan_cong_thi
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE phan_cong_thi;

-- Bảng cán bộ coi thi
CREATE TABLE IF NOT EXISTS can_bo (
    stt       INT,
    ma_gv     VARCHAR(50)  NOT NULL,
    ho_ten    VARCHAR(100),
    ngay_sinh VARCHAR(20),
    don_vi    VARCHAR(100),
    PRIMARY KEY (ma_gv)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng phòng thi
CREATE TABLE IF NOT EXISTS phong_thi (
    stt       INT,
    ten_phong VARCHAR(50) NOT NULL,
    ghi_chu  VARCHAR(100),
    PRIMARY KEY (ten_phong)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng kết quả phân công giám thị
CREATE TABLE IF NOT EXISTS ket_qua_phan_cong (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    ca_thi    INT,
    ma_gv     VARCHAR(50),
    ho_ten    VARCHAR(100),
    loai_gt   VARCHAR(20),   -- 'Giám thị 1' hoặc 'Giám thị 2'
    ten_phong VARCHAR(50)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Bảng kết quả giám sát hành lang
CREATE TABLE IF NOT EXISTS ket_qua_giam_sat (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    ca_thi   INT,
    ma_gv    VARCHAR(50),
    ho_ten   VARCHAR(100),
    phong_gs VARCHAR(200)   -- Ví dụ: 'Từ 101 đến 110'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT 'Database da khoi tao thanh cong!' AS status;
