package server;

import common.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final String workDir;
    private final Consumer<String> serverLogger;

    public ClientHandler(Socket socket, String workDir, Consumer<String> serverLogger) {
        this.socket       = socket;
        this.workDir      = workDir;
        this.serverLogger = serverLogger;
    }

    @Override
    public void run() {
        String clientInfo = socket.getRemoteSocketAddress().toString();
        log("[Server] Kết nối từ: " + clientInfo);

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            DataInputStream  dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream())
        ) {
            String line = reader.readLine();
            if (line == null) return;
            line = line.trim();
            log("[Server] Lệnh nhận: " + line);

            if (line.startsWith(Protocol.CMD_PROCESS)) {
                String[] parts = line.split(":");
                int n = parts.length > 1 ? parseInt(parts[1]) : 0;
                int m = parts.length > 2 ? parseInt(parts[2]) : 0;
                int k = parts.length > 3 ? parseInt(parts[3]) : 1;

                log(String.format("[Server] Tham số: n=%d phòng, m=%d giám thị, k=%d ca", n, m, k));

                writer.println(Protocol.RESP_READY);
                writer.flush();

                String canBoPath = workDir + File.separator + Protocol.FILE_CANBO;
                receiveFile(dis, canBoPath, "cán bộ");
                sendLog(writer, "Đã nhận file cán bộ (" + new File(canBoPath).length() + " bytes)");

                String phongPath = workDir + File.separator + Protocol.FILE_PHONG;
                receiveFile(dis, phongPath, "phòng thi");
                sendLog(writer, "Đã nhận file phòng thi (" + new File(phongPath).length() + " bytes)");

                sendLog(writer, "Đang đọc dữ liệu Excel...");
                List<CanBo>    canBoAll  = ExcelHandler.readCanBo(canBoPath);
                List<PhongThi> phongAll  = ExcelHandler.readPhongThi(phongPath);

                List<CanBo>    canBoList = n > 0 && m > 0
                    ? canBoAll.subList(0, Math.min(m, canBoAll.size()))
                    : canBoAll;
                List<PhongThi> phongList = n > 0
                    ? phongAll.subList(0, Math.min(n, phongAll.size()))
                    : phongAll;

                sendLog(writer, String.format("Sử dụng: %d cán bộ, %d phòng thi", canBoList.size(), phongList.size()));

                boolean dbOk = DatabaseHelper.testConnection();
                if (dbOk) {
                    sendLog(writer, "Đang lưu dữ liệu vào MySQL...");
                    try {
                        DatabaseHelper.importCanBo(new ArrayList<>(canBoList), canBoList.size());
                        DatabaseHelper.importPhongThi(new ArrayList<>(phongList), phongList.size());
                        sendLog(writer, "✅ Đã lưu vào CSDL MySQL.");
                    } catch (Exception e) {
                        sendLog(writer, "⚠️ Lỗi DB (tiếp tục không dùng DB): " + e.getMessage());
                        dbOk = false;
                    }
                } else {
                    sendLog(writer, "⚠️ Không kết nối được MySQL - tiếp tục không dùng DB.");
                }

                sendLog(writer, "Đang thực hiện phân công " + k + " ca thi...");
                long t0 = System.currentTimeMillis();
                List<PhanCong> dsPC = new ArrayList<>();
                List<GiamSat>  dsGS = new ArrayList<>();
                PhanCongAlgorithm.phanCongTatCaCa(new ArrayList<>(canBoList), new ArrayList<>(phongList), k, dsPC, dsGS);
                long elapsed = System.currentTimeMillis() - t0;
                sendLog(writer, String.format("✅ Phân công xong: %d bản ghi PC, %d bản ghi GS (%dms)", dsPC.size(), dsGS.size(), elapsed));

                if (dbOk) {
                    try {
                        DatabaseHelper.saveKetQua(dsPC, dsGS);
                        sendLog(writer, "✅ Đã lưu kết quả vào CSDL.");
                    } catch (Exception e) {
                        sendLog(writer, "⚠️ Lỗi lưu DB: " + e.getMessage());
                    }
                }

                String pcPath = workDir + File.separator + Protocol.FILE_PHANCONG;
                String gsPath = workDir + File.separator + Protocol.FILE_GIAMSAT;
                sendLog(writer, "Đang xuất file Excel kết quả...");
                ExcelHandler.writePhanCong(dsPC, pcPath);
                ExcelHandler.writeGiamSat(dsGS, gsPath);
                sendLog(writer, "✅ Xuất file Excel thành công!");

                log("[Server] File kết quả: " + pcPath + ", " + gsPath);

                writer.println("READY_PHANCONG");
                writer.flush();
                sendFile(dos, pcPath);

                writer.println("READY_GIAMSAT");
                writer.flush();
                sendFile(dos, gsPath);

                writer.println(Protocol.RESP_DONE);
                writer.flush();
                log("[Server] Hoàn thành xử lý cho " + clientInfo);

            } else {
                writer.println(Protocol.RESP_ERROR + ":Lệnh không hợp lệ: " + line);
            }

        } catch (Exception e) {
            log("[Server] Lỗi kết nối " + clientInfo + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            log("[Server] Đã đóng kết nối: " + clientInfo);
        }
    }

    private void receiveFile(DataInputStream dis, String savePath, String label) throws IOException {
        long fileSize = dis.readLong();
        log("[Server] Nhận file " + label + ": " + fileSize + " bytes → " + savePath);
        byte[] buf = new byte[Protocol.BUFFER_SIZE];
        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            long remaining = fileSize;
            while (remaining > 0) {
                int toRead = (int) Math.min(buf.length, remaining);
                int read   = dis.read(buf, 0, toRead);
                if (read < 0) break;
                fos.write(buf, 0, read);
                remaining -= read;
            }
        }
    }

    private void sendFile(DataOutputStream dos, String filePath) throws IOException {
        File file = new File(filePath);
        dos.writeLong(file.length());
        dos.flush();
        byte[] buf = new byte[Protocol.BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            while ((read = fis.read(buf)) > 0) {
                dos.write(buf, 0, read);
            }
        }
        dos.flush();
        log("[Server] Đã gửi file: " + filePath + " (" + file.length() + " bytes)");
    }

    private void sendLog(PrintWriter writer, String msg) {
        writer.println(Protocol.RESP_LOG + ":" + msg);
        writer.flush();
        log(msg);
    }

    private void log(String msg) {
        System.out.println(msg);
        if (serverLogger != null) serverLogger.accept(msg);
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
