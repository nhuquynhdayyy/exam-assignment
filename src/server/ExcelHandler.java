package server;

import common.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelHandler {

    // ==================== 1. ĐỌC FILE CÁN BỘ (ĐÚNG CỘT THEO ẢNH) ====================
    public static List<CanBo> readCanBo(String filePath) throws IOException {
        List<CanBo> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; }
                if (isEmptyRow(row)) continue;
                
                CanBo cb = new CanBo();
                // Index 0: STT | 1: Họ tên (B) | 2: Ngày sinh (C) | 3: Mã GV (D) | 4: Đơn vị (E)
                cb.setHoTen(safeStr(row.getCell(1)));   // Cột B
                cb.setNgaySinh(safeDate(row.getCell(2))); // Cột C
                cb.setMaGV(safeStr(row.getCell(3)));    // Cột D
                cb.setDonVi(safeStr(row.getCell(4)));   // Cột E
                
                if (!cb.getMaGV().isBlank()) list.add(cb);
            }
        }
        return list;
    }

    public static List<PhongThi> readPhongThi(String filePath) throws IOException {
        List<PhongThi> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; }
                if (isEmptyRow(row)) continue;
                PhongThi pt = new PhongThi();
                pt.setStt(safeInt(row.getCell(0)));
                pt.setTenPhong(safeStr(row.getCell(1)));
                pt.setDiaDiem(safeStr(row.getCell(2)));
                if (!pt.getTenPhong().isBlank()) list.add(pt);
            }
        }
        return list;
    }

    // ==================== 2. GHI FILE PHÂN CÔNG (CHIA SHEET THEO CA) ====================
    public static void writePhanCong(List<PhanCong> allList, String filePath) throws IOException {
        Map<Integer, List<PhanCong>> byCa = new TreeMap<>(); // TreeMap để sắp xếp Ca 1, 2, 3...
        for (PhanCong pc : allList) {
            byCa.computeIfAbsent(pc.getCaThi(), k -> new ArrayList<>()).add(pc);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle altStyle = createAltStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            for (Map.Entry<Integer, List<PhanCong>> entry : byCa.entrySet()) {
                // TẠO SHEET MỚI CHO MỖI CA
                Sheet sheet = wb.createSheet("Ca " + entry.getKey()); 
                
                // Vẽ Header (STT, Mã GV, Họ tên, GT1, GT2, Phòng)
                Row row0 = sheet.createRow(0);
                createMergedCell(sheet, wb, row0, 0, 0, 1, "STT", headerStyle);
                createMergedCell(sheet, wb, row0, 1, 0, 1, "Mã GV", headerStyle);
                createMergedCell(sheet, wb, row0, 2, 0, 1, "Họ và tên", headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 4));
                Cell gtHeader = row0.createCell(3);
                gtHeader.setCellValue("GIÁM THỊ");
                gtHeader.setCellStyle(headerStyle);
                createMergedCell(sheet, wb, row0, 5, 0, 1, "Phòng thi", headerStyle);

                Row row1 = sheet.createRow(1);
                row1.createCell(3).setCellValue("Giám thị 1");
                row1.createCell(4).setCellValue("Giám thị 2");
                row1.getCell(3).setCellStyle(headerStyle);
                row1.getCell(4).setCellStyle(headerStyle);

                // Group theo phòng để ghi 2 giám thị vào 2 dòng chung STT
                Map<String, List<PhanCong>> byPhong = new LinkedHashMap<>();
                for (PhanCong pc : entry.getValue()) {
                    byPhong.computeIfAbsent(pc.getTenPhong(), k -> new ArrayList<>()).add(pc);
                }

                int rowNum = 2;
                int sttCount = 1;
                for (var e : byPhong.entrySet()) {
                    for (PhanCong pc : e.getValue()) {
                        Row r = sheet.createRow(rowNum++);
                        CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;
                        setCell(r, 0, sttCount++, style); // STT 1, 2, 3...
                        setCell(r, 1, pc.getMaGV(), style);
                        setCell(r, 2, pc.getHoTen(), style);
                        setCell(r, 3, pc.getVaiTro().contains("1") ? "X" : "", centerStyle);
                        setCell(r, 4, pc.getVaiTro().contains("2") ? "X" : "", centerStyle);
                        setCell(r, 5, pc.getTenPhong(), style);
                    }
                }
                for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) { wb.write(fos); }
        }
    }

    // ==================== 3. GHI FILE GIÁM SÁT (ĐÃ THAM KHẢO CODE PHÂN CÔNG - CHIA SHEET THEO CA) ====================
    public static void writeGiamSat(List<GiamSat> allList, String filePath) throws IOException {
        // 1. Nhóm theo ca (Dùng TreeMap để tự động sắp xếp Ca 1 -> Ca 2 -> Ca 3)
        Map<Integer, List<GiamSat>> byCa = new TreeMap<>();
        for (GiamSat gs : allList) {
            byCa.computeIfAbsent(gs.getCaThi(), k -> new ArrayList<>()).add(gs);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle altStyle = createAltStyle(wb);

            // 2. Duyệt qua từng ca trong Map để tạo Sheet riêng
            for (Map.Entry<Integer, List<GiamSat>> entry : byCa.entrySet()) {
                int caThi = entry.getKey();
                List<GiamSat> dsGSTheoCa = entry.getValue();

                // TẠO SHEET MỚI Y HỆT CODE PHÂN CÔNG
                Sheet sheet = wb.createSheet("Ca " + caThi); 

                // Vẽ Header cho từng Sheet
                String[] headers = {"STT", "Mã GV", "Họ và tên", "Phòng thi được giám sát"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // 3. Ghi dữ liệu giám sát của riêng ca này vào sheet
                int rowNum = 1;
                int sttTrongCa = 1; // Reset STT về 1 khi sang sheet mới
                for (GiamSat gs : dsGSTheoCa) {
                    Row row = sheet.createRow(rowNum++);
                    CellStyle currentStyle = (rowNum % 2 == 0) ? altStyle : dataStyle;
                    
                    setCell(row, 0, sttTrongCa++, currentStyle); // STT tịnh tiến 1, 2, 3...
                    setCell(row, 1, gs.getMaGV(), currentStyle);
                    setCell(row, 2, gs.getHoTen(), currentStyle);
                    setCell(row, 3, gs.getPhongGiamSat(), currentStyle);
                }

                // Tự động căn chỉnh độ rộng cột cho từng sheet
                for (int i = 0; i < 4; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // 4. Xuất file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
        System.out.println("[Excel] Đã đồng bộ chia ca cho file Giám sát.");
    }

    // --- CÁC HÀM TRỢ GIÚP (HELPER) ---
    private static void createMergedCell(Sheet s, Workbook wb, Row r, int col, int r1, int r2, String val, CellStyle st) {
        s.addMergedRegion(new CellRangeAddress(r1, r2, col, col));
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(st);
    }
    private static void setCell(Row r, int col, Object val, CellStyle st) {
        Cell c = r.createCell(col);
        if (val instanceof Integer) c.setCellValue((Integer) val);
        else c.setCellValue(val != null ? val.toString() : "");
        c.setCellStyle(st);
    }
    private static String safeStr(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf((long)c.getNumericCellValue());
        return c.getStringCellValue().trim();
    }
    private static int safeInt(Cell c) {
        if (c == null) return 0;
        if (c.getCellType() == CellType.NUMERIC) return (int)c.getNumericCellValue();
        try { return Integer.parseInt(c.getStringCellValue()); } catch(Exception e) { return 0; }
    }
    private static String safeDate(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) return new SimpleDateFormat("dd/MM/yyyy").format(c.getDateCellValue());
        return safeStr(c);
    }
    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        return true;
    }
    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f); s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER); s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN); return s;
    }
    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setBorderBottom(BorderStyle.THIN); return s;
    }
    private static CellStyle createAltStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND); s.setBorderBottom(BorderStyle.THIN); return s;
    }
    private static CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND); s.setBorderBottom(BorderStyle.THIN); return s;
    }
}