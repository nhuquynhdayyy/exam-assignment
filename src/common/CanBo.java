package common;

import java.io.Serializable;

public class CanBo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int stt;
    private String maGV;
    private String hoTen;
    private String ngaySinh;
    private String donVi;

    public CanBo() {}

    public CanBo(int stt, String maGV, String hoTen, String ngaySinh, String donVi) {
        this.stt = stt;
        this.maGV = maGV;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.donVi = donVi;
    }

    public int getStt() { return stt; }
    public void setStt(int stt) { this.stt = stt; }

    public String getMaGV() { return maGV; }
    public void setMaGV(String maGV) { this.maGV = maGV; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(String ngaySinh) { this.ngaySinh = ngaySinh; }

    public String getDonVi() { return donVi; }
    public void setDonVi(String donVi) { this.donVi = donVi; }

    @Override
    public String toString() {
        return maGV + " - " + hoTen;
    }
}
