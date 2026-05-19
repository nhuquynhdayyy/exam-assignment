package server;

import common.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelHandler {

    // ==================== 1. ĐỌC FILE CÁN BỘ ====================
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
                // A=0(STT), B=1(Họ Tên), C=2(Ngày sinh), D=3(Mã GV), E=4(Đơn vị)
                cb.setHoTen(safeStr(row.getCell(1)));   
                cb.setNgaySinh(safeDate(row.getCell(2))); 
                cb.setMaGV(safeStr(row.getCell(3)));    
                cb.setDonVi(safeStr(row.getCell(4)));   
                
                if (!cb.getMaGV().isBlank()) list.add(cb);
            }
        }
        return list;
    }

    // ==================== 2. ĐỌC FILE PHÒNG THI ====================
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
                // A=0(STT), B=1(Phòng thi), C=2(Ghi chú/Địa điểm)
                pt.setStt(safeInt(row.getCell(0)));
                pt.setTenPhong(safeStr(row.getCell(1)));
                pt.setDiaDiem(safeStr(row.getCell(2)));
                
                if (!pt.getTenPhong().isBlank()) list.add(pt);
            }
        }
        return list;
    }

    // ==================== 3. GHI FILE PHÂN CÔNG ====================
    public static void writePhanCong(List<PhanCong> allList, String filePath) throws IOException {
        Map<Integer, List<PhanCong>> byCa = new LinkedHashMap<>();
        for (PhanCong pc : allList) {
            byCa.computeIfAbsent(pc.getCaThi(), k -> new ArrayList<>()).add(pc);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle altStyle = createAltStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            for (Map.Entry<Integer, List<PhanCong>> entry : byCa.entrySet()) {
                Sheet sheet = wb.createSheet("Ca " + entry.getKey());
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

                Map<String, PhanCong[]> byPhong = new LinkedHashMap<>();
                for (PhanCong pc : entry.getValue()) {
                    PhanCong[] pair = byPhong.computeIfAbsent(pc.getTenPhong(), k -> new PhanCong[2]);
                    if ("Giám thị 1".equals(pc.getVaiTro())) pair[0] = pc;
                    else pair[1] = pc;
                }

                int rowNum = 2;
                int rowStt = 1; 
                for (Map.Entry<String, PhanCong[]> e : byPhong.entrySet()) {
                    PhanCong gt1 = e.getValue()[0];
                    PhanCong gt2 = e.getValue()[1];

                    Row r1 = sheet.createRow(rowNum++);
                    setCell(r1, 0, rowStt++, (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r1, 1, gt1.getMaGV(), (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r1, 2, gt1.getHoTen(), (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r1, 3, "X", centerStyle);
                    setCell(r1, 4, "", (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r1, 5, e.getKey(), (rowNum % 2 == 0) ? altStyle : dataStyle);

                    Row r2 = sheet.createRow(rowNum++);
                    setCell(r2, 0, rowStt++, (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r2, 1, gt2.getMaGV(), (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r2, 2, gt2.getHoTen(), (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r2, 3, "", (rowNum % 2 == 0) ? altStyle : dataStyle);
                    setCell(r2, 4, "X", centerStyle);
                    setCell(r2, 5, e.getKey(), (rowNum % 2 == 0) ? altStyle : dataStyle);
                }
                for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) { wb.write(fos); }
        }
    }

    // ==================== 4. GHI FILE GIÁM SÁT ====================
    public static void writeGiamSat(List<GiamSat> allList, String filePath) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            
            Sheet sheet = wb.createSheet("GiamSat");
            String[] headers = {"STT", "Mã GV", "Họ và tên", "Phòng thi được giám sát"};
            Row hRow = sheet.createRow(0);
            for(int i=0; i<headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (GiamSat gs : allList) {
                Row r = sheet.createRow(rowNum++);
                setCell(r, 0, gs.getStt(), dataStyle);
                setCell(r, 1, gs.getMaGV(), dataStyle);
                setCell(r, 2, gs.getHoTen(), dataStyle);
                setCell(r, 3, gs.getPhongGiamSat(), dataStyle);
            }
            for(int i=0; i<4; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(filePath)) { wb.write(fos); }
        }
    }

    // ==================== CÁC HÀM HỖ TRỢ (HELPER) ====================
    private static void createMergedCell(Sheet sheet, Workbook wb, Row row, int col, int r1, int r2, String val, CellStyle style) {
        sheet.addMergedRegion(new CellRangeAddress(r1, r2, col, col));
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private static void setCell(Row row, int col, Object val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val instanceof Integer) c.setCellValue((Integer) val);
        else c.setCellValue(val.toString());
        c.setCellStyle(style);
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
        if (DateUtil.isCellDateFormatted(c)) return new SimpleDateFormat("dd/MM/yyyy").format(c.getDateCellValue());
        return safeStr(c);
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f); s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER); s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setBorderBottom(BorderStyle.THIN); return s;
    }

    private static CellStyle createAltStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN); return s;
    }

    private static CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN); return s;
    }
}