package client;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Client Swing — Giao diện phía Client
 *
 * FIX CHÍNH:
 * - KHÔNG dùng BufferedReader để đọc socket khi phía sau còn nhận file nhị phân.
 * - Dùng duy nhất DataInputStream để đọc cả text line và binary file.
 * - Hàm readLineUtf8(dis) tự đọc từng byte đến \n, không đọc lố sang dữ liệu file.
 */
public class ExamClient extends JFrame {

    // UI — Kết nối
    private JTextField txtServer;
    private JTextField txtPort;
    private JButton btnConnect;

    // UI — Tham số
    private JSpinner spinN;
    private JSpinner spinM;
    private JSpinner spinK;

    // UI — File
    private JTextField txtCanBoFile;
    private JTextField txtPhongFile;

    // UI — Kết quả
    private JButton btnRun;
    private JButton btnViewPC;
    private JButton btnViewGS;

    // UI — Log
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel lblStatus;

    // State
    private String canBoFilePath = "";
    private String phongFilePath = "";

    // Thư mục lưu kết quả = thư mục hiện tại, cùng chỗ chạy JAR
    private final String outputDir = System.getProperty("user.dir");

    public ExamClient() {
        super("Hệ thống Phân công Cán bộ Coi thi – Client");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(780, 700);
        setMinimumSize(new Dimension(720, 600));
        setLocationRelativeTo(null);
        setVisible(true);
        log("📁 Thư mục lưu kết quả: " + outputDir);
    }

    // ==================== BUILD UI ====================

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        Color primary = new Color(25, 100, 180);
        Color danger = new Color(190, 30, 30);
        Color bg = new Color(245, 248, 255);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBackground(bg);
        topPanel.setBorder(titledBorder("🔌 Kết nối Server", primary));

        topPanel.add(lbl("Server:"));
        txtServer = new JTextField(Protocol.HOST, 12);
        topPanel.add(txtServer);

        topPanel.add(lbl("Port:"));
        txtPort = new JTextField(String.valueOf(Protocol.PORT), 6);
        topPanel.add(txtPort);

        btnConnect = mkBtn("Kiểm tra kết nối", new Color(70, 130, 180), Color.WHITE);
        btnConnect.addActionListener(e -> testConnection());
        topPanel.add(btnConnect);

        add(topPanel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(4, 4, 4, 4));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        center.add(secLabel("📊 Bước 1: Nhập thông số ca thi"), g);

        g.gridwidth = 1;
        g.weightx = 0;
        g.gridx = 0;
        g.gridy = 1;
        center.add(lbl("Số phòng thi (n):"), g);

        g.gridx = 1;
        spinN = new JSpinner(new SpinnerNumberModel(1000, 1, 99999, 1));
        spinnerWidth(spinN, 90);
        center.add(spinN, g);

        g.gridx = 2;
        center.add(lbl("Số giám thị (m):"), g);

        g.gridx = 3;
        spinM = new JSpinner(new SpinnerNumberModel(2000, 1, 99999, 1));
        spinnerWidth(spinM, 90);
        center.add(spinM, g);

        g.gridx = 0;
        g.gridy = 2;
        center.add(lbl("Số ca thi (k):"), g);

        g.gridx = 1;
        spinK = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        spinnerWidth(spinK, 90);
        center.add(spinK, g);

        JLabel hint = new JLabel("  * n ≥ 1000, m ≥ 2n");
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        g.gridx = 2;
        g.gridwidth = 2;
        center.add(hint, g);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 4;
        center.add(new JSeparator(), g);

        g.gridy = 4;
        center.add(secLabel("📋 Bước 2: Chọn file danh sách cán bộ (CANBOCOITHI.xlsx)"), g);

        g.gridy = 5;
        g.gridwidth = 1;
        g.weightx = 0;
        g.gridx = 0;
        center.add(lbl("File CANBOCOITHI:"), g);

        g.gridx = 1;
        g.gridwidth = 2;
        g.weightx = 1;
        txtCanBoFile = new JTextField();
        txtCanBoFile.setEditable(false);
        center.add(txtCanBoFile, g);

        g.gridx = 3;
        g.gridwidth = 1;
        g.weightx = 0;
        JButton btnBrCB = mkBtn("Chọn...", primary, Color.WHITE);
        btnBrCB.addActionListener(e -> browseFile(txtCanBoFile, true));
        center.add(btnBrCB, g);

        g.gridx = 0;
        g.gridy = 6;
        g.gridwidth = 4;
        g.weightx = 0;
        center.add(secLabel("🏫 Bước 3: Chọn file danh sách phòng thi (PHONGTHI.xlsx)"), g);

        g.gridy = 7;
        g.gridwidth = 1;
        g.gridx = 0;
        center.add(lbl("File PHONGTHI:"), g);

        g.gridx = 1;
        g.gridwidth = 2;
        g.weightx = 1;
        txtPhongFile = new JTextField();
        txtPhongFile.setEditable(false);
        center.add(txtPhongFile, g);

        g.gridx = 3;
        g.gridwidth = 1;
        g.weightx = 0;
        JButton btnBrPT = mkBtn("Chọn...", primary, Color.WHITE);
        btnBrPT.addActionListener(e -> browseFile(txtPhongFile, false));
        center.add(btnBrPT, g);

        g.gridx = 0;
        g.gridy = 8;
        g.gridwidth = 4;
        center.add(new JSeparator(), g);

        g.gridy = 9;
        center.add(secLabel("⚙️ Bước 4: Thực hiện phân công & Xem kết quả"), g);

        g.gridy = 10;
        btnRun = mkBtn("🎯  GỬI DỮ LIỆU & THỰC HIỆN PHÂN CÔNG", danger, Color.WHITE);
        btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD, 14f));
        btnRun.setPreferredSize(new Dimension(400, 42));
        btnRun.addActionListener(e -> doProcess());
        center.add(btnRun, g);

        JPanel resultBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        resultBtnPanel.setBackground(Color.WHITE);

        btnViewPC = mkBtn("📂 Mở file Phân Công", new Color(210, 170, 0), Color.WHITE);
        btnViewPC.setEnabled(false);
        btnViewPC.addActionListener(e -> openFile(Protocol.FILE_PHANCONG));
        resultBtnPanel.add(btnViewPC);

        btnViewGS = mkBtn("📂 Mở file Giám Sát", new Color(210, 170, 0), Color.WHITE);
        btnViewGS.setEnabled(false);
        btnViewGS.addActionListener(e -> openFile(Protocol.FILE_GIAMSAT));
        resultBtnPanel.add(btnViewGS);

        g.gridy = 11;
        center.add(resultBtnPanel, g);

        add(center, BorderLayout.CENTER);

        JPanel botPanel = new JPanel(new BorderLayout(4, 4));
        botPanel.setBorder(titledBorder("📜 Nhật ký hoạt động", primary));
        botPanel.setBackground(Color.WHITE);

        logArea = new JTextArea(9, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(15, 15, 25));
        logArea.setForeground(new Color(180, 255, 180));
        botPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout(4, 0));
        statusBar.setBackground(Color.WHITE);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");

        lblStatus = new JLabel("  ✅ Sẵn sàng");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 12));

        statusBar.add(progressBar, BorderLayout.CENTER);
        statusBar.add(lblStatus, BorderLayout.EAST);
        botPanel.add(statusBar, BorderLayout.SOUTH);

        add(botPanel, BorderLayout.SOUTH);
    }

    // ==================== ACTIONS ====================

    private void testConnection() {
        runAsync("TestConn", () -> {
            String host = txtServer.getText().trim();
            int port = parsePort();

            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 3000);
                log("✅ Kết nối thành công tới " + host + ":" + port);
            } catch (Exception e) {
                log("❌ Không kết nối được: " + e.getMessage());
            }
        });
    }

    private void browseFile(JTextField target, boolean isCanBo) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            target.setText(f.getAbsolutePath());

            if (isCanBo) {
                canBoFilePath = f.getAbsolutePath();
            } else {
                phongFilePath = f.getAbsolutePath();
            }

            log("📄 Đã chọn: " + f.getName());
        }
    }

    private void doProcess() {
        if (canBoFilePath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file CANBOCOITHI.xlsx!", "Thiếu file", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (phongFilePath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file PHONGTHI.xlsx!", "Thiếu file", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int n = (Integer) spinN.getValue();
        int m = (Integer) spinM.getValue();
        int k = (Integer) spinK.getValue();

        runAsync("PhanCong", () -> {
            setControlsEnabled(false);
            btnViewPC.setEnabled(false);
            btnViewGS.setEnabled(false);

            try {
                log("─────────────────────────────────────────");
                log("🚀 Bắt đầu phân công: n=" + n + " phòng, m=" + m + " GV, k=" + k + " ca");
                progress(5, "Đang kết nối server...");

                try (Socket socket = createSocket();
                     DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                     DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

                    // Gửi lệnh PROCESS theo dạng text line để giữ tương thích với server hiện tại.
                    String cmd = Protocol.CMD_PROCESS + ":" + n + ":" + m + ":" + k;
                    writeLineUtf8(dos, cmd);
                    progress(10, "Chờ server sẵn sàng...");

                    String resp = readLineUtf8(dis);
                    if (!Protocol.RESP_READY.equals(resp)) {
                        log("❌ Server không sẵn sàng: " + resp);
                        return;
                    }

                    progress(15, "Đang gửi file cán bộ...");
                    sendFile(dos, canBoFilePath);
                    log("📤 Đã gửi CANBOCOITHI.xlsx");

                    progress(25, "Đang gửi file phòng thi...");
                    sendFile(dos, phongFilePath);
                    log("📤 Đã gửi PHONGTHI.xlsx");

                    boolean receivedPC = false;
                    boolean receivedGS = false;
                    int progressVal = 30;

                    while (true) {
                        String line = readLineUtf8(dis);
                        if (line == null) {
                            log("⚠️ Server đã đóng kết nối trước khi gửi DONE.");
                            break;
                        }

                        line = line.trim();

                        if (line.startsWith(Protocol.RESP_LOG + ":")) {
                            String msg = line.substring(Protocol.RESP_LOG.length() + 1);
                            log("   Server: " + msg);
                            if (progressVal < 70) {
                                progressVal += 5;
                                progress(progressVal, msg);
                            }
                        } else if ("READY_PHANCONG".equals(line)) {
                            progress(75, "Nhận file phân công...");
                            String savePath = outputDir + File.separator + Protocol.FILE_PHANCONG;
                            receiveFile(dis, savePath);
                            receivedPC = true;
                            log("✅ Đã nhận và lưu: " + savePath);
                        } else if ("READY_GIAMSAT".equals(line)) {
                            progress(90, "Nhận file giám sát...");
                            String savePath = outputDir + File.separator + Protocol.FILE_GIAMSAT;
                            receiveFile(dis, savePath);
                            receivedGS = true;
                            log("✅ Đã nhận và lưu: " + savePath);
                        } else if (Protocol.RESP_DONE.equals(line)) {
                            if (!receivedPC) {
                                log("⚠️ Chưa nhận được file phân công.");
                            }
                            if (!receivedGS) {
                                log("⚠️ Chưa nhận được file giám sát.");
                            }

                            progress(100, "Hoàn thành!");
                            log("🎉 Phân công thành công!");
                            log("📁 File kết quả lưu tại: " + outputDir);
                            log("─────────────────────────────────────────");

                            SwingUtilities.invokeLater(() -> {
                                btnViewPC.setEnabled(new File(outputDir, Protocol.FILE_PHANCONG).exists());
                                btnViewGS.setEnabled(new File(outputDir, Protocol.FILE_GIAMSAT).exists());
                                showDoneDialog();
                            });
                            break;
                        } else if (line.startsWith(Protocol.RESP_ERROR)) {
                            log("❌ " + line);
                            break;
                        } else {
                            log("⚠️ Phản hồi không xác định từ server: " + line);
                        }
                    }
                }
            } catch (Exception e) {
                log("❌ Lỗi: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setControlsEnabled(true);
            }
        });
    }

    // ==================== SOCKET HELPERS ====================

    /**
     * Gửi 1 dòng UTF-8 kết thúc bằng \n.
     * Dùng DataOutputStream duy nhất để tránh trộn nhiều writer trên cùng socket.
     */
    private void writeLineUtf8(DataOutputStream dos, String text) throws IOException {
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.write('\n');
        dos.flush();
    }

    /**
     * Đọc 1 dòng UTF-8 từ DataInputStream mà không buffer lố sang dữ liệu file nhị phân.
     * Đây là phần sửa lỗi chính thay cho BufferedReader.readLine().
     */
    private String readLineUtf8(DataInputStream dis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (true) {
            int b;
            try {
                b = dis.read();
            } catch (EOFException e) {
                return baos.size() == 0 ? null : baos.toString(StandardCharsets.UTF_8);
            }

            if (b == -1) {
                return baos.size() == 0 ? null : baos.toString(StandardCharsets.UTF_8);
            }

            if (b == '\n') {
                break;
            }

            if (b != '\r') {
                baos.write(b);
            }
        }

        return baos.toString(StandardCharsets.UTF_8);
    }

    private void sendFile(DataOutputStream dos, String filePath) throws IOException {
        File file = new File(filePath);
        dos.writeLong(file.length());

        byte[] buf = new byte[Protocol.BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            while ((read = fis.read(buf)) > 0) {
                dos.write(buf, 0, read);
            }
        }

        dos.flush();
    }

    private void receiveFile(DataInputStream dis, String savePath) throws IOException {
        long fileSize = dis.readLong();
        if (fileSize < 0) {
            throw new IOException("Kích thước file không hợp lệ: " + fileSize);
        }

        File outFile = new File(savePath);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        byte[] buf = new byte[Protocol.BUFFER_SIZE];
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            long remaining = fileSize;

            while (remaining > 0) {
                int toRead = (int) Math.min(buf.length, remaining);
                int read = dis.read(buf, 0, toRead);

                if (read < 0) {
                    throw new EOFException("Mất kết nối khi đang nhận file: " + savePath);
                }

                fos.write(buf, 0, read);
                remaining -= read;
            }
        }
    }

    private Socket createSocket() throws IOException {
        String host = txtServer.getText().trim();
        int port = parsePort();

        Socket s = new Socket(host, port);
        s.setSoTimeout(300_000);
        return s;
    }

    // ==================== FILE / UI HELPERS ====================

    private void openFile(String fileName) {
        File f = new File(outputDir + File.separator + fileName);

        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "Chưa có file: " + f.getAbsolutePath(), "Không tìm thấy", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(f);
        } catch (Exception e) {
            log("❌ Không mở được file: " + e.getMessage());
        }
    }

    private void showDoneDialog() {
        int choice = JOptionPane.showOptionDialog(
            this,
            "Phân công hoàn tất!\nFile kết quả đã lưu tại:\n" + outputDir,
            "Thành công",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new String[]{"Mở file Phân Công", "Mở file Giám Sát", "Đóng"},
            "Mở file Phân Công"
        );

        if (choice == 0) {
            openFile(Protocol.FILE_PHANCONG);
        } else if (choice == 1) {
            openFile(Protocol.FILE_GIAMSAT);
        }
    }

    private int parsePort() {
        try {
            return Integer.parseInt(txtPort.getText().trim());
        } catch (Exception e) {
            return Protocol.PORT;
        }
    }

    private void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void progress(int pct, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(pct);
            progressBar.setString(text);
            lblStatus.setText("  " + (pct >= 100 ? "✅" : "⏳") + " " + text);
        });
    }

    /**
     * Không override JFrame.setEnabled(boolean) nữa.
     * Hàm cũ tên setEnabled(boolean) dễ gây lỗi và khó hiểu.
     */
    private void setControlsEnabled(boolean en) {
        SwingUtilities.invokeLater(() -> {
            btnRun.setEnabled(en);
            btnConnect.setEnabled(en);
        });
    }

    private void runAsync(String name, Runnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log("❌ Lỗi [" + name + "]: " + e.getMessage());
            }
        }, name).start();
    }

    private JButton mkBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setOpaque(true);
        btn.setBorder(new CompoundBorder(
            new LineBorder(bg.darker(), 1),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel lbl(String text) {
        return new JLabel(text);
    }

    private JLabel secLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setForeground(new Color(25, 100, 180));
        return l;
    }

    private TitledBorder titledBorder(String title, Color color) {
        TitledBorder b = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color, 1),
            title
        );
        b.setTitleColor(color);
        b.setTitleFont(new Font("Arial", Font.BOLD, 12));
        return b;
    }

    private void spinnerWidth(JSpinner sp, int w) {
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(w / 10);
    }

    // ==================== MAIN ====================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(ExamClient::new);
    }
}
