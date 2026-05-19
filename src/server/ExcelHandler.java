package server;

import common.CanBo;
import common.GiamSat;
import common.PhanCong;
import common.PhongThi;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Đọc/ghi Excel (.xlsx) dùng Apache POI
 * - Ghi file PHANCONG: mỗi ca → một Sheet riêng (đúng như C# gốc)
 * - Ghi file GIAMSAT:  mỗi ca → một Sheet riêng
 */
public class ExcelHandler {

    // ==================== ĐỌC FILE ====================

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
                cb.setStt    (safeInt(row.getCell(0)));
                cb.setMaGV   (safeStr(row.getCell(1)));
                cb.setHoTen  (safeStr(row.getCell(2)));
                cb.setNgaySinh(safeDate(row.getCell(3)));
                cb.setDonVi  (safeStr(row.getCell(4)));
                if (cb.getMaGV() != null && !cb.getMaGV().isBlank()) list.add(cb);
            }
        }
        System.out.println("[Excel] Đọc " + list.size() + " cán bộ từ " + filePath);
        return list;
    }

    public static List<PhongThi> readPhongThi(String filePath) throws IOException {
        List<PhongThi> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getNumberOfSheets() > 1 ? wb.getSheetAt(1) : wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; }
                if (isEmptyRow(row)) continue;
                PhongThi pt = new PhongThi();
                pt.setStt     (safeInt(row.getCell(0)));
                pt.setTenPhong(safeStr(row.getCell(1)));
                pt.setDiaDiem (safeStr(row.getCell(2)));
                if (pt.getTenPhong() != null && !pt.getTenPhong().isBlank()) list.add(pt);
            }
        }
        System.out.println("[Excel] Đọc " + list.size() + " phòng thi từ " + filePath);
        return list;
    }

    // ==================== GHI FILE PHANCONG (multi-sheet theo ca) ====================

    /**
     * Ghi DANHSACHPHANCONG.xlsx — mỗi ca là một Sheet
     * Format theo đề bài: STT | Mã GV | Họ tên | Giám thị 1 (X) | Giám thị 2 (X) | Phòng thi
     */
    public static void writePhanCong(List<PhanCong> allList, String filePath) throws IOException {
        // Nhóm theo ca
        Map<Integer, List<PhanCong>> byCa = new LinkedHashMap<>();
        for (PhanCong pc : allList) {
            byCa.computeIfAbsent(pc.getCaThi(), k -> new ArrayList<>()).add(pc);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle subHeaderStyle = createSubHeaderStyle(wb);
            CellStyle dataStyle  = createDataStyle(wb);
            CellStyle altStyle   = createAltStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            for (Map.Entry<Integer, List<PhanCong>> entry : byCa.entrySet()) {
                int ca = entry.getKey();
                List<PhanCong> caList = entry.getValue();
                Sheet sheet = wb.createSheet("Ca " + ca);

                // Row 0: Header chính (merged cells)
                Row row0 = sheet.createRow(0);
                // A1:A2 = STT
                createMergedCell(sheet, wb, row0, 0, 0, 1, "STT", headerStyle);
                // B1:B2 = Mã GV
                createMergedCell(sheet, wb, row0, 1, 0, 1, "Mã GV", headerStyle);
                // C1:C2 = Họ và tên
                createMergedCell(sheet, wb, row0, 2, 0, 1, "Họ và tên", headerStyle);
                // D1:E1 = GIÁM THỊ (merged 2 cột)
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 4));
                Cell gtHeader = row0.createCell(3);
                gtHeader.setCellValue("GIÁM THỊ");
                gtHeader.setCellStyle(headerStyle);
                // F1:F2 = Phòng thi
                createMergedCell(sheet, wb, row0, 5, 0, 1, "Phòng thi", headerStyle);

                // Row 1: Sub-header
                Row row1 = sheet.createRow(1);
                for (int c = 0; c < 6; c++) {
                    Cell cell = row1.createCell(c);
                    if (c == 3) cell.setCellValue("Giám thị 1");
                    else if (c == 4) cell.setCellValue("Giám thị 2");
                    else cell.setCellValue("");
                    cell.setCellStyle(c == 3 || c == 4 ? subHeaderStyle : headerStyle);
                }

                // Nhóm theo phòng để ghép GT1 và GT2 vào một dòng
                Map<String, PhanCong[]> byPhong = new LinkedHashMap<>();
                for (PhanCong pc : caList) {
                    PhanCong[] pair = byPhong.computeIfAbsent(pc.getTenPhong(), k -> new PhanCong[2]);
                    if ("Giám thị 1".equals(pc.getVaiTro())) pair[0] = pc;
                    else pair[1] = pc;
                }

                int rowNum = 2, stt = 1;
                for (Map.Entry<String, PhanCong[]> e : byPhong.entrySet()) {
                    PhanCong gt1 = e.getValue()[0];
                    PhanCong gt2 = e.getValue()[1];
                    CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;

                    // Dòng GT1
                    Row r1 = sheet.createRow(rowNum++);
                    setCell(r1, 0, stt, style);
                    setCell(r1, 1, gt1 != null ? gt1.getMaGV() : "", style);
                    setCell(r1, 2, gt1 != null ? gt1.getHoTen() : "", style);
                    setCell(r1, 3, "X", centerStyle);  // Giám thị 1
                    setCell(r1, 4, "",  style);          // Giám thị 2
                    setCell(r1, 5, e.getKey(), style);

                    // Dòng GT2
                    CellStyle style2 = (rowNum % 2 == 0) ? altStyle : dataStyle;
                    Row r2 = sheet.createRow(rowNum++);
                    setCell(r2, 0, stt++, style2);
                    setCell(r2, 1, gt2 != null ? gt2.getMaGV() : "", style2);
                    setCell(r2, 2, gt2 != null ? gt2.getHoTen() : "", style2);
                    setCell(r2, 3, "",  style2);         // Giám thị 1
                    setCell(r2, 4, "X", centerStyle);   // Giám thị 2
                    setCell(r2, 5, e.getKey(), style2);
                }

                // Auto size columns
                int[] colWidths = {8, 15, 25, 14, 14, 12};
                for (int i = 0; i < 6; i++) {
                    sheet.setColumnWidth(i, colWidths[i] * 256);
                }
                sheet.setDefaultRowHeight((short) 350);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
        System.out.println("[Excel] Ghi " + allList.size() + " dòng phân công → " + filePath);
    }

    // ==================== GHI FILE GIAMSAT (multi-sheet theo ca) ====================

    public static void writeGiamSat(List<GiamSat> allList, String filePath) throws IOException {
        Map<Integer, List<GiamSat>> byCa = new LinkedHashMap<>();
        for (GiamSat gs : allList) {
            byCa.computeIfAbsent(gs.getCaThi(), k -> new ArrayList<>()).add(gs);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle altStyle    = createAltStyle(wb);

            for (Map.Entry<Integer, List<GiamSat>> entry : byCa.entrySet()) {
                int ca = entry.getKey();
                List<GiamSat> caList = entry.getValue();
                Sheet sheet = wb.createSheet("Ca " + ca);

                String[] headers = {"STT", "Mã GV", "Họ và tên", "Phòng thi được giám sát"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                for (GiamSat gs : caList) {
                    Row row = sheet.createRow(rowNum);
                    CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;
                    setCell(row, 0, gs.getStt(), style);
                    setCell(row, 1, gs.getMaGV(), style);
                    setCell(row, 2, gs.getHoTen(), style);
                    setCell(row, 3, gs.getPhongGiamSat(), style);
                    rowNum++;
                }

                int[] colWidths = {8, 15, 25, 35};
                for (int i = 0; i < 4; i++) {
                    sheet.setColumnWidth(i, colWidths[i] * 256);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
        System.out.println("[Excel] Ghi " + allList.size() + " dòng giám sát → " + filePath);
    }

    // ==================== HELPERS ====================

    private static void createMergedCell(Sheet sheet, Workbook wb, Row row,
                                          int col, int firstRow, int lastRow,
                                          String value, CellStyle style) {
        if (firstRow < lastRow) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, col, col));
        }
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private static CellStyle createSubHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
            new byte[]{(byte) 31, (byte) 73, (byte) 125}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private static CellStyle createAltStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private static CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    private static void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private static String safeStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private static int safeInt(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(safeStr(cell)); } catch (Exception e) { return 0; }
    }

    private static String safeDate(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
        }
        return safeStr(cell);
    }

    private static void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Integer i) cell.setCellValue(i);
        else if (value instanceof Double d) cell.setCellValue(d);
        else cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(style);
    }
}
