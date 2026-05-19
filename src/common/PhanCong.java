package common;

import java.io.Serializable;

/**
 * Kết quả phân công giám thị cho một phòng thi
 */
public class PhanCong implements Serializable {
    private static final long serialVersionUID = 1L;

    private int stt;
    private String maGV;
    private String hoTen;
    private String vaiTro;   // "Giám thị 1" hoặc "Giám thị 2"
    private String tenPhong;
    private int caThi;       // 1, 2, 3, ...

    public PhanCong() {}

    public PhanCong(int stt, String maGV, String hoTen, String vaiTro, String tenPhong, int caThi) {
        this.stt = stt;
        this.maGV = maGV;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
        this.tenPhong = tenPhong;
        this.caThi = caThi;
    }

    public int getStt() { return stt; }
    public void setStt(int stt) { this.stt = stt; }

    public String getMaGV() { return maGV; }
    public void setMaGV(String maGV) { this.maGV = maGV; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }

    public String getTenPhong() { return tenPhong; }
    public void setTenPhong(String tenPhong) { this.tenPhong = tenPhong; }

    public int getCaThi() { return caThi; }
    public void setCaThi(int caThi) { this.caThi = caThi; }
}
