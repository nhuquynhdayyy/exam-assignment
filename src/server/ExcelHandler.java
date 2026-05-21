package server;

import common.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelHandler {

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
                cb.setHoTen(safeStr(row.getCell(1)));
                cb.setNgaySinh(safeDate(row.getCell(2)));
                cb.setMaGV(safeStr(row.getCell(3)));
                cb.setDonVi(safeStr(row.getCell(4)));
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

    public static void writePhanCong(List<PhanCong> allList, String filePath) throws IOException {
        final int MAX_PHONG_PER_SHEET = 15;

        Map<Integer, List<PhanCong>> byCa = new TreeMap<>();
        for (PhanCong pc : allList) {
            byCa.computeIfAbsent(pc.getCaThi(), k -> new ArrayList<>()).add(pc);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle  = createSimpleHeaderStyle(wb);
            CellStyle dataStyle    = createSimpleDataStyle(wb);
            CellStyle centerStyle  = createSimpleCenterStyle(wb);

            for (Map.Entry<Integer, List<PhanCong>> entry : byCa.entrySet()) {
                int caThi = entry.getKey();

                Map<String, List<PhanCong>> byPhong = new LinkedHashMap<>();
                for (PhanCong pc : entry.getValue()) {
                    byPhong.computeIfAbsent(pc.getTenPhong(), k -> new ArrayList<>()).add(pc);
                }

                List<String> phongKeys = new ArrayList<>(byPhong.keySet());
                int totalPhong = phongKeys.size();
                int trangIndex = 1;

                for (int start = 0; start < totalPhong; start += MAX_PHONG_PER_SHEET) {
                    int end = Math.min(start + MAX_PHONG_PER_SHEET, totalPhong);
                    List<String> phongTrang = phongKeys.subList(start, end);

                    String sheetName = "Ca " + caThi + " Trang " + trangIndex++;
                    Sheet sheet = wb.createSheet(sheetName);

                    Row row0 = sheet.createRow(0);
                    createMergedCell(sheet, wb, row0, 0, 0, 1, "STT",        headerStyle);
                    createMergedCell(sheet, wb, row0, 1, 0, 1, "Mã GV",      headerStyle);
                    createMergedCell(sheet, wb, row0, 2, 0, 1, "Họ và tên",  headerStyle);
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 4));
                    Cell gtHeader = row0.createCell(3);
                    gtHeader.setCellValue("GIÁM THỊ");
                    gtHeader.setCellStyle(headerStyle);
                    createMergedCell(sheet, wb, row0, 5, 0, 1, "Phòng thi",  headerStyle);

                    Row row1 = sheet.createRow(1);
                    Cell c3 = row1.createCell(3); c3.setCellValue("Giám thị 1"); c3.setCellStyle(headerStyle);
                    Cell c4 = row1.createCell(4); c4.setCellValue("Giám thị 2"); c4.setCellStyle(headerStyle);

                    int rowNum    = 2;
                    int sttCount  = 1;
                    for (String tenPhong : phongTrang) {
                        for (PhanCong pc : byPhong.get(tenPhong)) {
                            Row r = sheet.createRow(rowNum++);
                            setCell(r, 0, sttCount++,                              dataStyle);
                            setCell(r, 1, pc.getMaGV(),                            dataStyle);
                            setCell(r, 2, pc.getHoTen(),                           dataStyle);
                            setCell(r, 3, pc.getVaiTro().contains("1") ? "X" : "", centerStyle);
                            setCell(r, 4, pc.getVaiTro().contains("2") ? "X" : "", centerStyle);
                            setCell(r, 5, pc.getTenPhong(),                        dataStyle);
                        }
                    }

                    for (int i = 0; i < 6; i++) {
                        sheet.autoSizeColumn(i);
                        if (i == 5) sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1500);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    public static void writeGiamSat(List<GiamSat> allList, String filePath) throws IOException {
        Map<Integer, List<GiamSat>> byCa = new TreeMap<>();
        for (GiamSat gs : allList) {
            byCa.computeIfAbsent(gs.getCaThi(), k -> new ArrayList<>()).add(gs);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createSimpleHeaderStyle(wb);
            CellStyle dataStyle = createSimpleDataStyle(wb);

            for (Map.Entry<Integer, List<GiamSat>> entry : byCa.entrySet()) {
                Sheet sheet = wb.createSheet("Ca " + entry.getKey()); 
                String[] headers = {"STT", "Mã GV", "Họ và tên", "Phòng thi được giám sát"};
                Row hRow = sheet.createRow(0);
                for(int i=0; i<headers.length; i++) {
                    Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle);
                }

                int rowNum = 1;
                for (GiamSat gs : entry.getValue()) {
                    Row r = sheet.createRow(rowNum++);
                    setCell(r, 0, rowNum - 1, dataStyle);
                    setCell(r, 1, gs.getMaGV(), dataStyle);
                    setCell(r, 2, gs.getHoTen(), dataStyle);
                    setCell(r, 3, gs.getPhongGiamSat(), dataStyle);
                }
                for(int i=0; i<4; i++) {
                    sheet.autoSizeColumn(i);
                    if (i == 3) sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 2000);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) { wb.write(fos); }
        }
    }

    private static CellStyle createSimpleHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle createSimpleDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle createSimpleCenterStyle(Workbook wb) {
        CellStyle s = createSimpleDataStyle(wb);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

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
}