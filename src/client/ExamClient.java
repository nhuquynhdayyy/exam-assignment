package client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import common.Protocol;


/**
 * Client Swing - giao dien phia Client.
 *
 * Luu y ky thuat:
 * - Khong dung BufferedReader de doc socket khi phia sau con nhan file nhi phan.
 * - Dung DataInputStream de doc ca text line va binary file.
 * - readLineUtf8(dis) doc tung byte den \n, tranh doc lo sang du lieu file.
 */
public class ExamClient extends JFrame {


    private static final Color COLOR_BG = new Color(244, 247, 251);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(30, 92, 168);
    private static final Color COLOR_PRIMARY_DARK = new Color(22, 68, 126);
    private static final Color COLOR_ACCENT = new Color(16, 137, 114);
    private static final Color COLOR_DANGER = new Color(196, 67, 67);
    private static final Color COLOR_WARNING = new Color(180, 125, 20);
    private static final Color COLOR_TEXT = new Color(32, 41, 57);
    private static final Color COLOR_MUTED = new Color(99, 112, 130);
    private static final Color COLOR_BORDER = new Color(219, 226, 236);
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);


    // UI - ket noi
    private JTextField txtServer;
    private JTextField txtPort;
    private JButton btnConnect;


    // UI - tham so
    private JSpinner spinN;
    private JSpinner spinM;
    private JSpinner spinK;


    // UI - file
    private JTextField txtCanBoFile;
    private JTextField txtPhongFile;


    // UI - ket qua
    private JButton btnRun;
    private JButton btnViewPC;
    private JButton btnViewGS;


    // UI - log
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblCanBoState;
    private JLabel lblPhongState;


    // State
    private String canBoFilePath = "";
    private String phongFilePath = "";


    // Thu muc luu ket qua = thu muc hien tai, cung cho chay JAR
    private final String outputDir = System.getProperty("user.dir");


    public ExamClient() {
        super("Hệ thống Phân công Cán bộ Coi thi - Client");
        configureLookAndFeel();
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        applyInitialWindowBounds();
        setVisible(true);
        log("Thu mục lưu kết quả: " + outputDir);
    }


    // ==================== BUILD UI ====================


    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);
        getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));


        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }


    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));


        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);


        JLabel title = new JLabel("Phân công cán bộ coi thi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(COLOR_TEXT);


        JLabel subtitle = new JLabel("Nhập tham số, gửi dữ liệu Excel lên server và nhận file kết quả phân công.");
        subtitle.setFont(FONT_UI);
        subtitle.setForeground(COLOR_MUTED);


        textPanel.add(title);
        textPanel.add(subtitle);
        header.add(textPanel, BorderLayout.WEST);


        JLabel badge = new JLabel("CLIENT");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(COLOR_PRIMARY_DARK);
        badge.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 210, 238), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
        header.add(badge, BorderLayout.EAST);
        return header;
    }


    private JComponent buildMainContent() {
        JPanel formColumn = new WidthTrackingPanel();
        formColumn.setOpaque(false);
        formColumn.setLayout(new GridBagLayout());
        GridBagConstraints formG = new GridBagConstraints();
        formG.gridx = 0;
        formG.weightx = 1;
        formG.fill = GridBagConstraints.HORIZONTAL;
        formG.anchor = GridBagConstraints.NORTH;
        formG.insets = new Insets(0, 0, 8, 0);


        formG.gridy = 0;
        formColumn.add(buildConnectionCard(), formG);
        formG.gridy = 1;
        formColumn.add(buildParamCardCompact(), formG);
        formG.gridy = 2;
        formColumn.add(buildFileCard(), formG);
        formG.gridy = 3;
        formColumn.add(buildActionCard(), formG);


        formG.gridy = 4;
        formG.weighty = 1;
        formG.insets = new Insets(0, 0, 0, 0);
        formColumn.add(Box.createVerticalGlue(), formG);


        JScrollPane formScroll = new JScrollPane(formColumn);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setMinimumSize(new Dimension(520, 0));
        formScroll.setPreferredSize(new Dimension(620, 520));


        JPanel logCard = buildLogCard();
        logCard.setMinimumSize(new Dimension(340, 0));
        logCard.setPreferredSize(new Dimension(390, 520));


        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formScroll, logCard);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.62);
        split.setDividerSize(10);
        return split;
    }


    private JPanel buildConnectionCard() {
        JPanel card = card("Kết nối server", "Kiểm tra server trước khi gửi dữ liệu xử lý.");
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = baseConstraints();


        g.gridx = 0;
        row.add(label("Server"), g);
        g.gridx = 1;
        g.weightx = 1;
        txtServer = textField(Protocol.HOST);
        txtServer.setPreferredSize(new Dimension(170, 38));
        row.add(txtServer, g);


        g.gridx = 2;
        g.weightx = 0;
        row.add(label("Port"), g);
        g.gridx = 3;
        txtPort = textField(String.valueOf(Protocol.PORT));
        txtPort.setColumns(6);
        txtPort.setPreferredSize(new Dimension(78, 38));
        row.add(txtPort, g);


        g.gridx = 4;
        btnConnect = button("Kiểm tra", COLOR_PRIMARY);
        btnConnect.setPreferredSize(new Dimension(118, 38));
        btnConnect.addActionListener(e -> testConnection());
        row.add(btnConnect, g);


        card.add(row, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildParamCardCompact() {
        JPanel card = card("Thông số phân công", "Nhập số phòng, số cán bộ và số ca thi cần xử lý.");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);


        spinN = spinner(1000, 1, 99999);
        spinM = spinner(2000, 1, 99999);
        spinK = spinner(3, 1, 100);


        GridBagConstraints g = baseConstraints();
        g.insets = new Insets(0, 0, 8, 10);
        g.weightx = 0.5;


        g.gridx = 0;
        g.gridy = 0;
        grid.add(fieldBlock("Số phòng thi (n)", spinN), g);


        g.gridx = 1;
        grid.add(fieldBlock("Số giám thị (m)", spinM), g);


        g.gridx = 0;
        g.gridy = 1;
        grid.add(fieldBlock("Số ca thi (k)", spinK), g);


        JLabel rule = new JLabel("<html><b>Quy tắc:</b> n >= 1000, m >= 2n. Mỗi phòng cần 2 giám thị trong từng ca.</html>");
        rule.setFont(FONT_UI);
        rule.setForeground(COLOR_MUTED);
        rule.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 222, 196), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        g.gridx = 1;
        grid.add(rule, g);


        card.add(grid, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildParamCard() {
        JPanel card = card("Thông số phân công", "Điền số phòng, số cán bộ và số ca thi cần xử lý.");
        JPanel grid = new JPanel(new GridLayout(3, 2, 10, 10));
        grid.setOpaque(false);


        spinN = spinner(1000, 1, 99999);
        spinM = spinner(2000, 1, 99999);
        spinK = spinner(3, 1, 100);


        grid.add(fieldBlock("Số phòng thi (n)", spinN));
        grid.add(fieldBlock("Số giám thị (m)", spinM));
        grid.add(fieldBlock("Số ca thi (k)", spinK));


        JLabel rule = new JLabel("<html><b>Quy tắc:</b> n ≥ 1000, m ≥ 2n. Mỗi phòng cần 2 giám thị trong từng ca.</html>");
        rule.setFont(FONT_UI);
        rule.setForeground(COLOR_MUTED);
        rule.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 222, 196), 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        grid.add(rule);


        card.add(grid, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildFileCard() {
        JPanel card = card("Dữ liệu đầu vào", "Chọn đúng hai file Excel trước khi thực hiện phân công.");
        JPanel files = new JPanel();
        files.setOpaque(false);
        files.setLayout(new BoxLayout(files, BoxLayout.Y_AXIS));


        txtCanBoFile = textField("");
        txtCanBoFile.setEditable(false);
        lblCanBoState = stateLabel("Chưa chọn");
        JButton btnBrCB = button("Chọn file", COLOR_PRIMARY);
        btnBrCB.addActionListener(e -> browseFile(txtCanBoFile, true));
        files.add(fileRow("CANBOCOITHI.xlsx", txtCanBoFile, lblCanBoState, btnBrCB));


        files.add(Box.createVerticalStrut(10));


        txtPhongFile = textField("");
        txtPhongFile.setEditable(false);
        lblPhongState = stateLabel("Chưa chọn");
        JButton btnBrPT = button("Chọn file", COLOR_PRIMARY);
        btnBrPT.addActionListener(e -> browseFile(txtPhongFile, false));
        files.add(fileRow("PHONGTHI.xlsx", txtPhongFile, lblPhongState, btnBrPT));


        card.add(files, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildActionCard() {
        JPanel card = card("Thực hiện và kết quả", "Gửi dữ liệu lên server, sau đó mở file Excel kết quả ngay từ màn hình này.");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);


        btnRun = button("Gửi dữ liệu và phân công", COLOR_DANGER);
        btnRun.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnRun.setPreferredSize(new Dimension(240, 44));
        btnRun.addActionListener(e -> doProcess());
        content.add(btnRun, BorderLayout.NORTH);


        JPanel resultPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        resultPanel.setOpaque(false);


        btnViewPC = button("Mở file Phân công", COLOR_WARNING);
        btnViewPC.setEnabled(false);
        btnViewPC.addActionListener(e -> openFile(Protocol.FILE_PHANCONG));
        resultPanel.add(btnViewPC);


        btnViewGS = button("Mở file Giám sát", COLOR_WARNING);
        btnViewGS.setEnabled(false);
        btnViewGS.addActionListener(e -> openFile(Protocol.FILE_GIAMSAT));
        resultPanel.add(btnViewGS);


        resultPanel.setPreferredSize(new Dimension(240, 40));
        content.add(resultPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }


    private JPanel buildLogCard() {
        JPanel card = card("Nhật ký xử lý", "Theo dõi phản hồi từ server và tiến trình nhận file.");


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


    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(12, 0));
        statusBar.setOpaque(false);
        statusBar.setBorder(new EmptyBorder(8, 0, 0, 0));


        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        progressBar.setFont(FONT_UI_BOLD);
        progressBar.setForeground(COLOR_ACCENT);
        progressBar.setPreferredSize(new Dimension(100, 24));


        lblStatus = new JLabel("Sẵn sàng");
        lblStatus.setFont(FONT_UI_BOLD);
        lblStatus.setForeground(COLOR_TEXT);


        statusBar.add(progressBar, BorderLayout.CENTER);
        statusBar.add(lblStatus, BorderLayout.EAST);
        return statusBar;
    }


    // ==================== ACTIONS ====================


    private void testConnection() {
        runAsync("TestConn", () -> {
            String host = txtServer.getText().trim();
            int port = parsePort();
            progress(8, "Đang kiểm tra kết nối...");


            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 3000);
                progress(100, "Kết nối server thành công");
                log("Kết nối thành công tới " + host + ":" + port);
            } catch (Exception e) {
                progress(0, "Không kết nối được server");
                log("Không kết nối được: " + e.getMessage());
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
                lblCanBoState.setText("Đã chọn");
                lblCanBoState.setForeground(COLOR_ACCENT);
            } else {
                phongFilePath = f.getAbsolutePath();
                lblPhongState.setText("Đã chọn");
                lblPhongState.setForeground(COLOR_ACCENT);
            }


            log("Đã chọn: " + f.getName());
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
                log("------------------------------------------------------------");
                log("Bắt đầu phân công: n=" + n + " phòng, m=" + m + " giám thị, k=" + k + " ca");
                progress(5, "Đang kết nối server...");


                try (Socket socket = createSocket();
                     DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                     DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {


                    // Gui lenh PROCESS dang text line de giu tuong thich voi server hien tai.
                    String cmd = Protocol.CMD_PROCESS + ":" + n + ":" + m + ":" + k;
                    writeLineUtf8(dos, cmd);
                    progress(10, "Chờ server sẵn sàng...");


                    String resp = readLineUtf8(dis);
                    if (!Protocol.RESP_READY.equals(resp)) {
                        log("Server không sẵn sàng: " + resp);
                        return;
                    }


                    progress(15, "Đang gửi file cán bộ...");
                    sendFile(dos, canBoFilePath);
                    log("Đã gửi CANBOCOITHI.xlsx");


                    progress(25, "Đang gửi file phòng thi...");
                    sendFile(dos, phongFilePath);
                    log("Đã gửi PHONGTHI.xlsx");


                    boolean receivedPC = false;
                    boolean receivedGS = false;
                    int progressVal = 30;


                    while (true) {
                        String line = readLineUtf8(dis);
                        if (line == null) {
                            log("Server đã đóng kết nối trước khi gửi DONE.");
                            break;
                        }


                        line = line.trim();


                        if (line.startsWith(Protocol.RESP_LOG + ":")) {
                            String msg = line.substring(Protocol.RESP_LOG.length() + 1);
                            log("Server: " + msg);
                            if (progressVal < 70) {
                                progressVal += 5;
                                progress(progressVal, msg);
                            }
                        } else if ("READY_PHANCONG".equals(line)) {
                            progress(75, "Nhận file phân công...");
                            String savePath = outputDir + File.separator + Protocol.FILE_PHANCONG;
                            receiveFile(dis, savePath);
                            receivedPC = true;
                            log("Đã nhận và lưu: " + savePath);
                        } else if ("READY_GIAMSAT".equals(line)) {
                            progress(90, "Nhận file giám sát...");
                            String savePath = outputDir + File.separator + Protocol.FILE_GIAMSAT;
                            receiveFile(dis, savePath);
                            receivedGS = true;
                            log("Đã nhận và lưu: " + savePath);
                        } else if (Protocol.RESP_DONE.equals(line)) {
                            if (!receivedPC) {
                                log("Chưa nhận được file phân công.");
                            }
                            if (!receivedGS) {
                                log("Chưa nhận được file giám sát.");
                            }


                            progress(100, "Hoàn thành");
                            log("Phân công thành công.");
                            log("File kết quả lưu tại: " + outputDir);
                            log("------------------------------------------------------------");


                            SwingUtilities.invokeLater(() -> {
                                btnViewPC.setEnabled(new File(outputDir, Protocol.FILE_PHANCONG).exists());
                                btnViewGS.setEnabled(new File(outputDir, Protocol.FILE_GIAMSAT).exists());
                                showDoneDialog();
                            });
                            break;
                        } else if (line.startsWith(Protocol.RESP_ERROR)) {
                            log(line);
                            break;
                        } else {
                            log("Phản hồi không xác định từ server: " + line);
                        }
                    }
                }
            } catch (Exception e) {
                log("Lỗi: " + e.getMessage());
                e.printStackTrace();
            } finally {
                setControlsEnabled(true);
            }
        });
    }


    // ==================== SOCKET HELPERS ====================


    private void writeLineUtf8(DataOutputStream dos, String text) throws IOException {
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.write('\n');
        dos.flush();
    }


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
            log("Không mở được file: " + e.getMessage());
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
            new String[]{"Mở file Phân công", "Mở file Giám sát", "Đóng"},
            "Mở file Phân công"
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
            lblStatus.setText((pct >= 100 ? "Hoàn tất: " : "Trạng thái: ") + text);
        });
    }


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
                log("Lỗi [" + name + "]: " + e.getMessage());
            }
        }, name).start();
    }


    private JPanel card(String title, String description) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(COLOR_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(12, 12, 12, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getMaximumSize().height));


        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 1));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(COLOR_TEXT);
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(FONT_UI);
        descLabel.setForeground(COLOR_MUTED);
        heading.add(titleLabel);
        heading.add(descLabel);
        card.add(heading, BorderLayout.NORTH);


        return card;
    }


    private GridBagConstraints baseConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.CENTER;
        return g;
    }


    private JPanel fieldBlock(String title, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setMinimumSize(new Dimension(0, 62));
        panel.add(label(title), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }


    private JPanel fileRow(String title, JTextField field, JLabel state, JButton button) {
        JPanel panel = new JPanel(new BorderLayout(8, 6));
        panel.setOpaque(false);


        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label(title), BorderLayout.WEST);
        top.add(state, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);


        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(field, BorderLayout.CENTER);
        inputRow.add(button, BorderLayout.EAST);
        panel.add(inputRow, BorderLayout.CENTER);


        return panel;
    }


    private JTextField textField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(FONT_UI);
        field.setForeground(COLOR_TEXT);
        field.setMinimumSize(new Dimension(0, 38));
        field.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }


    private JSpinner spinner(int value, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        spinner.setFont(FONT_UI);
        spinner.setPreferredSize(new Dimension(150, 34));
        spinner.setMinimumSize(new Dimension(120, 34));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setColumns(8);
        editor.getTextField().setFont(FONT_UI);
        editor.getTextField().setBorder(new EmptyBorder(6, 8, 6, 8));
        spinner.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        return spinner;
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


    private JLabel stateLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_UI);
        label.setForeground(COLOR_MUTED);
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


    private void applyInitialWindowBounds() {
        Rectangle workArea = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();


        int width = Math.min(1120, Math.max(900, workArea.width - 80));
        int height = Math.min(700, Math.max(580, workArea.height - 80));
        int x = workArea.x + (workArea.width - width) / 2;
        int y = workArea.y + (workArea.height - height) / 2;


        setBounds(x, y, width, height);
    }


    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }


        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }


        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 32, 16);
        }


        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }


        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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