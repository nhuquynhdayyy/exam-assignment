package common;

public class Protocol {
    public static final int    PORT        = 9999;
    public static final String HOST        = "localhost";

    public static final String CMD_PROCESS     = "PROCESS";
    public static final String CMD_SEND_CANBO  = "SEND_CANBO";
    public static final String CMD_SEND_PHONG  = "SEND_PHONG";

    public static final String RESP_OK         = "OK";
    public static final String RESP_READY      = "READY";
    public static final String RESP_ERROR      = "ERROR";
    public static final String RESP_DONE       = "DONE";
    public static final String RESP_LOG        = "LOG";

    public static final int BUFFER_SIZE = 65536;

    public static final String FILE_CANBO    = "CANBOCOITHI.xlsx";
    public static final String FILE_PHONG    = "PHONGTHI.xlsx";
    public static final String FILE_PHANCONG = "DANHSACHPHANCONG.xlsx";
    public static final String FILE_GIAMSAT  = "DANHSACHGIAMSAT.xlsx";
}
