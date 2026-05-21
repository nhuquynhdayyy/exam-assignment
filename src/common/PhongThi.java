package common;

import java.io.Serializable;

public class PhongThi implements Serializable {
    private static final long serialVersionUID = 1L;

    private int stt;
    private String tenPhong;
    private String diaDiem;

    public PhongThi() {}

    public PhongThi(int stt, String tenPhong, String diaDiem) {
        this.stt = stt;
        this.tenPhong = tenPhong;
        this.diaDiem = diaDiem;
    }

    public int getStt() { return stt; }
    public void setStt(int stt) { this.stt = stt; }

    public String getTenPhong() { return tenPhong; }
    public void setTenPhong(String tenPhong) { this.tenPhong = tenPhong; }

    public String getDiaDiem() { return diaDiem; }
    public void setDiaDiem(String diaDiem) { this.diaDiem = diaDiem; }

    @Override
    public String toString() {
        return tenPhong + " (" + diaDiem + ")";
    }
}
