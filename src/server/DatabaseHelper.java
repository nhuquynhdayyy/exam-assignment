package server;

import common.CanBo;
import common.GiamSat;
import common.PhanCong;
import common.PhongThi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private static final String URL      = "jdbc:mysql://localhost:3306/phan_cong_thi?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void initSchema() throws SQLException {
        Connection conn = getConnection();
        String[] sqls = {
            """
            CREATE TABLE IF NOT EXISTS can_bo (
                stt INT,
                ma_gv VARCHAR(50) NOT NULL,
                ho_ten VARCHAR(100),
                ngay_sinh VARCHAR(20),
                don_vi VARCHAR(100),
                PRIMARY KEY (ma_gv)
            ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS phong_thi (
                stt INT,
                ten_phong VARCHAR(50) NOT NULL,
                ghi_chu VARCHAR(100),
                PRIMARY KEY (ten_phong)
            ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ket_qua_phan_cong (
                id INT AUTO_INCREMENT PRIMARY KEY,
                ca_thi INT,
                ma_gv VARCHAR(50),
                ho_ten VARCHAR(100),
                loai_gt VARCHAR(20),
                ten_phong VARCHAR(50)
            ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ket_qua_giam_sat (
                id INT AUTO_INCREMENT PRIMARY KEY,
                ca_thi INT,
                ma_gv VARCHAR(50),
                ho_ten VARCHAR(100),
                phong_gs VARCHAR(200)
            ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """
        };
        for (String sql : sqls) {
            conn.createStatement().execute(sql);
        }
        System.out.println("[DB] Schema đã khởi tạo xong.");
    }

    public static void importCanBo(List<CanBo> list, int m) throws SQLException {
        Connection conn = getConnection();
        conn.createStatement().execute("TRUNCATE TABLE can_bo");
        String sql = "INSERT INTO can_bo (stt, ma_gv, ho_ten, ngay_sinh, don_vi) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int count = 0;
            for (CanBo cb : list) {
                if (m > 0 && count >= m) break;
                ps.setInt   (1, cb.getStt());
                ps.setString(2, cb.getMaGV());
                ps.setString(3, cb.getHoTen());
                ps.setString(4, cb.getNgaySinh());
                ps.setString(5, cb.getDonVi());
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        System.out.println("[DB] Đã import " + Math.min(list.size(), m > 0 ? m : list.size()) + " cán bộ.");
    }

    public static void importPhongThi(List<PhongThi> list, int n) throws SQLException {
        Connection conn = getConnection();
        conn.createStatement().execute("TRUNCATE TABLE phong_thi");
        String sql = "INSERT INTO phong_thi (stt, ten_phong, ghi_chu) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int count = 0;
            for (PhongThi pt : list) {
                if (n > 0 && count >= n) break;
                ps.setInt   (1, pt.getStt());
                ps.setString(2, pt.getTenPhong());
                ps.setString(3, pt.getDiaDiem());
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        System.out.println("[DB] Đã import " + Math.min(list.size(), n > 0 ? n : list.size()) + " phòng thi.");
    }

    public static List<CanBo> getCanBo() throws SQLException {
        List<CanBo> list = new ArrayList<>();
        String sql = "SELECT stt, ma_gv, ho_ten, ngay_sinh, don_vi FROM can_bo ORDER BY stt";
        try (ResultSet rs = getConnection().createStatement().executeQuery(sql)) {
            while (rs.next()) {
                list.add(new CanBo(
                    rs.getInt("stt"),
                    rs.getString("ma_gv"),
                    rs.getString("ho_ten"),
                    rs.getString("ngay_sinh"),
                    rs.getString("don_vi")
                ));
            }
        }
        return list;
    }

    public static List<PhongThi> getPhongThi() throws SQLException {
        List<PhongThi> list = new ArrayList<>();
        String sql = "SELECT stt, ten_phong, ghi_chu FROM phong_thi ORDER BY stt";
        try (ResultSet rs = getConnection().createStatement().executeQuery(sql)) {
            while (rs.next()) {
                list.add(new PhongThi(
                    rs.getInt("stt"),
                    rs.getString("ten_phong"),
                    rs.getString("ghi_chu")
                ));
            }
        }
        return list;
    }

    public static void saveKetQua(List<PhanCong> dsPC, List<GiamSat> dsGS) throws SQLException {
        Connection conn = getConnection();
        conn.createStatement().execute("TRUNCATE TABLE ket_qua_phan_cong");
        conn.createStatement().execute("TRUNCATE TABLE ket_qua_giam_sat");

        String sqlPC = "INSERT INTO ket_qua_phan_cong (ca_thi, ma_gv, ho_ten, loai_gt, ten_phong) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlPC)) {
            for (PhanCong pc : dsPC) {
                ps.setInt   (1, pc.getCaThi());
                ps.setString(2, pc.getMaGV());
                ps.setString(3, pc.getHoTen());
                String vaiTro = pc.getVaiTro();
                int loaiGT = 0;
                if (vaiTro.equalsIgnoreCase("Giám thị 1")) {
                    loaiGT = 1;
                } else if (vaiTro.equalsIgnoreCase("Giám thị 2")) {
                    loaiGT = 2;
                }
                ps.setInt(4, loaiGT);
                ps.setString(5, pc.getTenPhong());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        String sqlGS = "INSERT INTO ket_qua_giam_sat (ca_thi, ma_gv, ho_ten, phong_gs) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlGS)) {
            for (GiamSat gs : dsGS) {
                ps.setInt   (1, gs.getCaThi());
                ps.setString(2, gs.getMaGV());
                ps.setString(3, gs.getHoTen());
                ps.setString(4, gs.getPhongGiamSat());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("[DB] Lưu kết quả: " + dsPC.size() + " phân công, " + dsGS.size() + " giám sát.");
    }

    public static List<PhanCong> getKetQuaPhanCong(int caThi) throws SQLException {
        List<PhanCong> list = new ArrayList<>();
        String sql = "SELECT ca_thi, ma_gv, ho_ten, loai_gt, ten_phong FROM ket_qua_phan_cong WHERE ca_thi=? ORDER BY ten_phong";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, caThi);
            try (ResultSet rs = ps.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    list.add(new PhanCong(stt++, rs.getString("ma_gv"), rs.getString("ho_ten"),
                        (rs.getInt("loai_gt") == 1 ? "Giám thị 1" : "Giám thị 2"), rs.getString("ten_phong"), rs.getInt("ca_thi")));
                }
            }
        }
        return list;
    }

    public static List<Integer> getDistinctCaThi() throws SQLException {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT DISTINCT ca_thi FROM ket_qua_phan_cong ORDER BY ca_thi";
        try (ResultSet rs = getConnection().createStatement().executeQuery(sql)) {
            while (rs.next()) list.add(rs.getInt(1));
        }
        return list;
    }

    public static boolean testConnection() {
        try {
            getConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
