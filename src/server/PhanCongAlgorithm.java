package server;

import common.*;
import java.util.*;

public class PhanCongAlgorithm {

    public static void phanCongTatCaCa(
            List<CanBo> canBoList,
            List<PhongThi> phongList,
            int soCa,
            List<PhanCong> outPC,
            List<GiamSat> outGS)
    {
        int n = phongList.size();
        int m = canBoList.size();

        for (int s = 1; s <= soCa; s++) {
            int shift = (s - 1) * n;
            Set<String> usedThisCa = new HashSet<>();

            // 1. Phân công Giám thị
            for (int i = 0; i < n; i++) {
                CanBo gt1 = canBoList.get((i + shift) % m);
                CanBo gt2 = canBoList.get((i + n + shift) % m);

                outPC.add(new PhanCong(0, gt1.getMaGV(), gt1.getHoTen(), "Giám thị 1", phongList.get(i).getTenPhong(), s));
                outPC.add(new PhanCong(0, gt2.getMaGV(), gt2.getHoTen(), "Giám thị 2", phongList.get(i).getTenPhong(), s));

                usedThisCa.add(gt1.getMaGV());
                usedThisCa.add(gt2.getMaGV());
            }

            // 2. Phân công Giám sát
            List<CanBo> gsPool = new ArrayList<>();
            for (CanBo cb : canBoList) {
                if (!usedThisCa.contains(cb.getMaGV())) gsPool.add(cb);
            }

            int soGS = gsPool.size();
            if (soGS > 0) {
                // Thuật toán chia kẹo: mỗi người ít nhất 'phongCoBan' phòng, 'phanDu' người đầu nhận thêm 1
                int phongCoBan = n / soGS; // 783 / 34 = 23
                int phanDu = n % soGS;     // 783 % 34 = 1
                
                int pointer = 0;
                for (int gi = 0; gi < soGS; gi++) {
                    CanBo gs = gsPool.get(gi);
                    int currentTake = phongCoBan + (gi < phanDu ? 1 : 0);
                    
                    int startIdx = pointer;
                    int endIdx = pointer + currentTake - 1;
                    
                    String range = String.format("Từ %s đến %s", 
                                    phongList.get(startIdx).getTenPhong(), 
                                    phongList.get(endIdx).getTenPhong());
                    
                    outGS.add(new GiamSat(gi + 1, gs.getMaGV(), gs.getHoTen(), range, s));
                    pointer += currentTake;
                }
            }
        }
    }
}