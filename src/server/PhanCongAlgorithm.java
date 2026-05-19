package server;

import common.CanBo;
import common.GiamSat;
import common.PhanCong;
import common.PhongThi;

import java.util.*;

/**
 * Thuật toán phân công cán bộ coi thi
 * Thuật toán xoay vòng đơn giản (giống bản C# gốc):
 *   - GT1 = cb[(i + shift) % m]
 *   - GT2 = cb[(i + n + shift) % m]
 *   - shift = (ca - 1) * n
 * Đảm bảo không trùng phòng, không trùng cặp qua các ca.
 */
public class PhanCongAlgorithm {

    /**
     * Phân công tất cả k ca thi cùng lúc
     * @param canBoList danh sách m cán bộ
     * @param phongList danh sách n phòng
     * @param soCa      số ca thi k
     * @param outPC     output danh sách phân công
     * @param outGS     output danh sách giám sát
     */
    public static void phanCongTatCaCa(
            List<CanBo> canBoList,
            List<PhongThi> phongList,
            int soCa,
            List<PhanCong> outPC,
            List<GiamSat> outGS)
    {
        int n = phongList.size();
        int m = canBoList.size();

        int soGiamThiCanThiet = n * 2;
        if (m < soGiamThiCanThiet) {
            throw new IllegalArgumentException(
                "Không đủ cán bộ: cần ít nhất " + soGiamThiCanThiet + " nhưng chỉ có " + m);
        }

        int sttPC = 1;
        int sttGS = 1;

        for (int s = 1; s <= soCa; s++) {
            // shift = (ca - 1) * n  → mỗi ca dịch n vị trí
            int shift = (s - 1) * n;
            Set<String> usedThisCa = new HashSet<>();

            // Phân công GT1 và GT2 cho mỗi phòng
            for (int i = 0; i < n; i++) {
                CanBo gt1 = canBoList.get((i + shift) % m);
                CanBo gt2 = canBoList.get((i + n + shift) % m);

                outPC.add(new PhanCong(sttPC++, gt1.getMaGV(), gt1.getHoTen(), "Giám thị 1",
                    phongList.get(i).getTenPhong(), s));
                outPC.add(new PhanCong(sttPC++, gt2.getMaGV(), gt2.getHoTen(), "Giám thị 2",
                    phongList.get(i).getTenPhong(), s));

                usedThisCa.add(gt1.getMaGV());
                usedThisCa.add(gt2.getMaGV());
            }

            // Cán bộ thừa → giám sát hành lang
            List<CanBo> gsPool = new ArrayList<>();
            for (CanBo cb : canBoList) {
                if (!usedThisCa.contains(cb.getMaGV())) gsPool.add(cb);
            }

            int soGS = gsPool.size();
            if (soGS > 0) {
                // Chia đều phòng cho mỗi giám sát
                int phongPerGS = Math.max(1, (int) Math.ceil((double) n / soGS));
                for (int gi = 0; gi < soGS; gi++) {
                    CanBo gs = gsPool.get(gi);
                    int startIdx = gi * phongPerGS;
                    int endIdx   = Math.min(startIdx + phongPerGS - 1, n - 1);

                    String range;
                    if (startIdx >= n) {
                        range = "Dự bị (hành lang chung)";
                    } else if (startIdx == endIdx) {
                        range = "Phòng " + phongList.get(startIdx).getTenPhong();
                    } else {
                        range = "Từ " + phongList.get(startIdx).getTenPhong()
                              + " đến " + phongList.get(endIdx).getTenPhong();
                    }
                    outGS.add(new GiamSat(sttGS++, gs.getMaGV(), gs.getHoTen(), range, s));
                }
            }
        }
    }
}
