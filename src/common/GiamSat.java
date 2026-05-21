package common;

import java.io.Serializable;

public class GiamSat implements Serializable {
    private static final long serialVersionUID = 1L;

    private int stt;
    private String maGV;
    private String hoTen;
    private String phongGiamSat;
    private int caThi;

    public GiamSat() {}

    public GiamSat(int stt, String maGV, String hoTen, String phongGiamSat, int caThi) {
        this.stt = stt;
        this.maGV = maGV;
        this.hoTen = hoTen;
        this.phongGiamSat = phongGiamSat;
        this.caThi = caThi;
    }

    public int getStt() { return stt; }
    public void setStt(int stt) { this.stt = stt; }

    public String getMaGV() { return maGV; }
    public void setMaGV(String maGV) { this.maGV = maGV; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getPhongGiamSat() { return phongGiamSat; }
    public void setPhongGiamSat(String phongGiamSat) { this.phongGiamSat = phongGiamSat; }

    public int getCaThi() { return caThi; }
    public void setCaThi(int caThi) { this.caThi = caThi; }
}
