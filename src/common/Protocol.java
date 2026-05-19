package common;

/**
 * Hằng số giao thức Client-Server (TCP Socket)
 *
 * Quy trình mới (gộp một lần gửi):
 *   Client → "PROCESS:{n}:{m}:{k}\n"  (n=phòng, m=giám thị, k=số ca)
 *   Server → "READY\n"
 *   Client → [long: canBoFileSize][bytes]
 *   Client → [long: phongFileSize][bytes]
 *   Server xử lý...
 *   Server → "READY_PHANCONG\n" + [long: size][bytes]
 *   Server → "READY_GIAMSAT\n"  + [long: size][bytes]
 *   Server → "DONE\n"
 */
public class Protocol {
    public static final int    PORT        = 9999;
    public static final String HOST        = "localhost";

    // Lệnh client → server
    public static final String CMD_PROCESS     = "PROCESS";   // PROCESS:{n}:{m}:{k}
    public static final String CMD_SEND_CANBO  = "SEND_CANBO"; // (giữ cho tương thích nếu cần)
    public static final String CMD_SEND_PHONG  = "SEND_PHONG";

    // Phản hồi server → client
    public static final String RESP_OK         = "OK";
    public static final String RESP_READY      = "READY";
    public static final String RESP_ERROR      = "ERROR";
    public static final String RESP_DONE       = "DONE";
    public static final String RESP_LOG        = "LOG";  // SERVER→CLIENT log message

    // Buffer
    public static final int BUFFER_SIZE = 65536;

    // Tên file trao đổi
    public static final String FILE_CANBO    = "CANBOCOITHI.xlsx";
    public static final String FILE_PHONG    = "PHONGTHI.xlsx";
    public static final String FILE_PHANCONG = "DANHSACHPHANCONG.xlsx";
    public static final String FILE_GIAMSAT  = "DANHSACHGIAMSAT.xlsx";
}
