package server;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Server GUI — Giao diện phía Server
 * Tính năng:
 *  - Nút Bắt đầu / Dừng server
 *  - Hiển thị log kết nối và xử lý
 *  - Kiểm tra trạng thái MySQL
 *  - Lưu file kết quả vào thư mục dự án (workDir)
 */
public class ExamServer extends JFrame {

    private static final String WORK_DIR = "server_data";

    // UI
    private JTextArea  logArea;
    private JButton    btnToggle;
    private JLabel     lblStatus;
    private JLabel     lblDbStatus;
    private JTextField txtPort;

    // State
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;
    private Thread acceptThread;

    public ExamServer() {
        super("Server Phân Công Cán Bộ Coi Thi");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(680, 520);
        setLocationRelativeTo(null);
        setVisible(true);
        checkDbStatus();
    }

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        Color primary = new Color(20, 80, 160);
        Color bg      = new Color(240, 245, 255);

        // ── Top panel: controls ──
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topPanel.setBackground(bg);
        topPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(primary), "⚙️ Điều khiển Server",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), primary));

        topPanel.add(new JLabel("Port:"));
        txtPort = new JTextField(String.valueOf(Protocol.PORT), 6);
        topPanel.add(txtPort);

        btnToggle = makeBtn("▶  BẮT ĐẦU SERVER", new Color(34, 139, 34), Color.WHITE);
        btnToggle.setPreferredSize(new Dimension(200, 38));
        btnToggle.addActionListener(e -> toggleServer());
        topPanel.add(btnToggle);

        lblStatus = new JLabel("  🔴 Chưa khởi động");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 13));
        topPanel.add(lblStatus);

        add(topPanel, BorderLayout.NORTH);

        // ── Center: log area ──
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(15, 15, 25));
        logArea.setForeground(new Color(180, 255, 180));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(primary), "📜 Nhật ký Server",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), primary));
        add(scroll, BorderLayout.CENTER);

        // ── Bottom: DB status + working dir ──
        JPanel botPanel = new JPanel(new BorderLayout(4, 0));
        botPanel.setBackground(bg);
        botPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        lblDbStatus = new JLabel("  🔄 Đang kiểm tra MySQL...");
        lblDbStatus.setFont(new Font("Arial", Font.PLAIN, 11));
        botPanel.add(lblDbStatus, BorderLayout.WEST);

        JLabel lblDir = new JLabel("📁 Thư mục làm việc: " + new File(WORK_DIR).getAbsolutePath() + "  ");
        lblDir.setFont(new Font("Arial", Font.PLAIN, 11));
        lblDir.setForeground(Color.DARK_GRAY);
        botPanel.add(lblDir, BorderLayout.EAST);

        add(botPanel, BorderLayout.SOUTH);
    }

    // ==================== SERVER LOGIC ====================

    private void toggleServer() {
        if (!running) startServer();
        else          stopServer();
    }

    private void startServer() {
        int port;
        try { port = Integer.parseInt(txtPort.getText().trim()); }
        catch (Exception e) { log("❌ Port không hợp lệ"); return; }

        new File(WORK_DIR).mkdirs();

        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            running = true;

            btnToggle.setText("⬛  DỪNG SERVER");
            btnToggle.setBackground(new Color(180, 30, 30));
            lblStatus.setText("  🟢 Đang chạy – Port " + port);

            log("╔══════════════════════════════════════╗");
            log("║   SERVER PHÂN CÔNG CÁN BỘ COI THI   ║");
            log("╠══════════════════════════════════════╣");
            log("║  Port     : " + port);
            log("║  Thư mục  : " + new File(WORK_DIR).getAbsolutePath());
            log("╚══════════════════════════════════════╝");
            log("Đang chờ kết nối từ client...\n");

            // Init DB schema
            try {
                DatabaseHelper.initSchema();
                log("✅ Đã khởi tạo schema MySQL.");
            } catch (Exception e) {
                log("⚠️ MySQL không khả dụng: " + e.getMessage());
            }

            // Accept thread
            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        client.setSoTimeout(120_000);
                        log("🔗 Kết nối mới: " + client.getRemoteSocketAddress());
                        pool.execute(new ClientHandler(client, WORK_DIR, this::log));
                    } catch (Exception e) {
                        if (running) log("Lỗi accept: " + e.getMessage());
                    }
                }
            }, "ServerAccept");
            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            log("❌ Không thể khởi động server: " + e.getMessage());
        }
    }

    private void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        if (pool != null) pool.shutdown();
        btnToggle.setText("▶  BẮT ĐẦU SERVER");
        btnToggle.setBackground(new Color(34, 139, 34));
        lblStatus.setText("  🔴 Đã dừng");
        log("⬛ Server đã dừng.\n");
    }

    private void checkDbStatus() {
        new Thread(() -> {
            boolean ok = DatabaseHelper.testConnection();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblDbStatus.setText("  🟢 MySQL: Kết nối OK");
                    lblDbStatus.setForeground(new Color(0, 128, 0));
                } else {
                    lblDbStatus.setText("  🔴 MySQL: Không kết nối được (sẽ chạy không dùng DB)");
                    lblDbStatus.setForeground(new Color(180, 0, 0));
                }
            });
        }).start();
    }

    // ==================== HELPERS ====================

    private void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + time + "] " + msg;
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private JButton makeBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new CompoundBorder(
            new LineBorder(bg.darker(), 1),
            new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ==================== MAIN ====================

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ExamServer::new);
    }
}
