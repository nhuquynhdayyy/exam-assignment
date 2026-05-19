package server;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import common.Protocol;


/**
 * Server GUI - giao dien phia Server.
 * Tinh nang:
 * - Nut Bat dau / Dung server
 * - Hien thi log ket noi va xu ly
 * - Kiem tra trang thai MySQL
 * - Luu file ket qua vao thu muc server_data
 */
public class ExamServer extends JFrame {


    private static final String WORK_DIR = "server_data";


    private static final Color COLOR_BG = new Color(244, 247, 251);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(30, 92, 168);
    private static final Color COLOR_SUCCESS = new Color(16, 137, 114);
    private static final Color COLOR_DANGER = new Color(196, 67, 67);
    private static final Color COLOR_TEXT = new Color(32, 41, 57);
    private static final Color COLOR_MUTED = new Color(99, 112, 130);
    private static final Color COLOR_BORDER = new Color(219, 226, 236);
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);


    // UI
    private JTextArea logArea;
    private JButton btnToggle;
    private JLabel lblStatus;
    private JLabel lblDbStatus;
    private JLabel lblDir;
    private JTextField txtPort;


    // State
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;
    private Thread acceptThread;


    public ExamServer() {
        super("Server Phân Công Cán Bộ Coi Thi");
        configureLookAndFeel();
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 600);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        setVisible(true);
        checkDbStatus();
    }


    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);
        getRootPane().setBorder(new EmptyBorder(16, 16, 16, 16));


        add(buildHeader(), BorderLayout.NORTH);
        add(buildLogCard(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }


    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 12));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 14, 0));


        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);


        JLabel title = new JLabel("Server phân công cán bộ coi thi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(COLOR_TEXT);
        JLabel subtitle = new JLabel("Nhận file Excel từ client, chạy phân công và trả lại file kết quả.");
        subtitle.setFont(FONT_UI);
        subtitle.setForeground(COLOR_MUTED);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        wrapper.add(titlePanel, BorderLayout.NORTH);


        JPanel controls = cardPanel(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(label("Port"));
        txtPort = textField(String.valueOf(Protocol.PORT));
        txtPort.setColumns(7);
        left.add(txtPort);


        btnToggle = button("Bắt đầu server", COLOR_SUCCESS);
        btnToggle.setPreferredSize(new Dimension(170, 38));
        btnToggle.addActionListener(e -> toggleServer());
        left.add(btnToggle);


        controls.add(left, BorderLayout.WEST);


        lblStatus = new JLabel("Chưa khởi động");
        lblStatus.setFont(FONT_UI_BOLD);
        lblStatus.setForeground(COLOR_DANGER);
        controls.add(lblStatus, BorderLayout.EAST);


        wrapper.add(controls, BorderLayout.CENTER);
        return wrapper;
    }


    private JPanel buildLogCard() {
        JPanel card = cardPanel(new BorderLayout(0, 12));


        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 2));
        heading.setOpaque(false);
        JLabel title = new JLabel("Nhật ký server");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(COLOR_TEXT);
        JLabel subtitle = new JLabel("Theo dõi client kết nối, tiến trình xử lý và trạng thái MySQL.");
        subtitle.setFont(FONT_UI);
        subtitle.setForeground(COLOR_MUTED);
        heading.add(title);
        heading.add(subtitle);
        card.add(heading, BorderLayout.NORTH);


        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(18, 24, 33));
        logArea.setForeground(new Color(215, 230, 245));
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(new EmptyBorder(12, 12, 12, 12));


        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(new LineBorder(new Color(31, 42, 58), 1, true));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(14, 0, 0, 0));


        lblDbStatus = new JLabel("Đang kiểm tra MySQL...");
        lblDbStatus.setFont(FONT_UI_BOLD);
        lblDbStatus.setForeground(COLOR_MUTED);
        bottom.add(infoPanel("Cơ sở dữ liệu", lblDbStatus));


        lblDir = new JLabel(new File(WORK_DIR).getAbsolutePath());
        lblDir.setFont(FONT_UI);
        lblDir.setForeground(COLOR_MUTED);
        bottom.add(infoPanel("Thư mục làm việc", lblDir));


        return bottom;
    }


    // ==================== SERVER LOGIC ====================


    private void toggleServer() {
        if (!running) {
            startServer();
        } else {
            stopServer();
        }
    }


    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (Exception e) {
            log("Port không hợp lệ.");
            return;
        }


        new File(WORK_DIR).mkdirs();


        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            running = true;


            btnToggle.setText("Dừng server");
            btnToggle.setBackground(COLOR_DANGER);
            btnToggle.setBorder(new CompoundBorder(
                new LineBorder(COLOR_DANGER.darker(), 1, true),
                new EmptyBorder(8, 14, 8, 14)
            ));
            lblStatus.setText("Đang chạy - Port " + port);
            lblStatus.setForeground(COLOR_SUCCESS);


            log("============================================================");
            log("SERVER PHÂN CÔNG CÁN BỘ COI THI");
            log("Port: " + port);
            log("Thư mục: " + new File(WORK_DIR).getAbsolutePath());
            log("Đang chờ kết nối từ client...");
            log("============================================================");


            try {
                DatabaseHelper.initSchema();
                log("Đã khởi tạo schema MySQL.");
            } catch (Exception e) {
                log("MySQL không khả dụng: " + e.getMessage());
            }


            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        client.setSoTimeout(120_000);
                        log("Kết nối mới: " + client.getRemoteSocketAddress());
                        pool.execute(new ClientHandler(client, WORK_DIR, this::log));
                    } catch (Exception e) {
                        if (running) {
                            log("Lỗi accept: " + e.getMessage());
                        }
                    }
                }
            }, "ServerAccept");
            acceptThread.setDaemon(true);
            acceptThread.start();


        } catch (IOException e) {
            log("Không thể khởi động server: " + e.getMessage());
        }
    }


    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        if (pool != null) {
            pool.shutdown();
        }
        btnToggle.setText("Bắt đầu server");
        btnToggle.setBackground(COLOR_SUCCESS);
        btnToggle.setBorder(new CompoundBorder(
            new LineBorder(COLOR_SUCCESS.darker(), 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));
        lblStatus.setText("Đã dừng");
        lblStatus.setForeground(COLOR_DANGER);
        log("Server đã dừng.");
    }


    private void checkDbStatus() {
        new Thread(() -> {
            boolean ok = DatabaseHelper.testConnection();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblDbStatus.setText("MySQL: Kết nối OK");
                    lblDbStatus.setForeground(COLOR_SUCCESS);
                } else {
                    lblDbStatus.setText("MySQL: Không kết nối được, vẫn chạy không dùng DB");
                    lblDbStatus.setForeground(COLOR_DANGER);
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


    private JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(COLOR_CARD);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }


    private JPanel infoPanel(String title, JLabel valueLabel) {
        JPanel panel = cardPanel(new BorderLayout(0, 6));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_UI_BOLD);
        titleLabel.setForeground(COLOR_TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }


    private JTextField textField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(FONT_UI);
        field.setForeground(COLOR_TEXT);
        field.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }


    private JButton button(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


                Color fill = isEnabled() ? getBackground() : new Color(235, 239, 245);
                if (isEnabled() && getModel().isPressed()) {
                    fill = fill.darker();
                } else if (isEnabled() && getModel().isRollover()) {
                    fill = fill.brighter();
                }


                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();


                Color oldForeground = getForeground();
                if (!isEnabled()) {
                    setForeground(new Color(73, 85, 102));
                }
                super.paintComponent(g);
                setForeground(oldForeground);
            }
        };
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_UI_BOLD);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(true);
        btn.setRolloverEnabled(true);
        btn.setBorder(new CompoundBorder(
            new LineBorder(bg.darker(), 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }


    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_UI_BOLD);
        label.setForeground(COLOR_TEXT);
        return label;
    }


    private void configureLookAndFeel() {
        UIManager.put("Label.font", FONT_UI);
        UIManager.put("Button.font", FONT_UI_BOLD);
        UIManager.put("TextField.font", FONT_UI);
        UIManager.put("Button.disabledText", new Color(73, 85, 102));
        UIManager.put("OptionPane.messageFont", FONT_UI);
        UIManager.put("OptionPane.buttonFont", FONT_UI_BOLD);
    }


    // ==================== MAIN ====================


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(ExamServer::new);
    }
}



