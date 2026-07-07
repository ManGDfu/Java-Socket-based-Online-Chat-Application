package common;

import java.io.Serializable;

/**
 * 文件/图片传输时的内存载体，供客户端读取本地文件后封装为 {@link Message}。
 */
public class FilePacket implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private long fileSize;
    private String fileType;
    private byte[] data;

    public FilePacket() {
    }

    public FilePacket(String fileName, long fileSize, String fileType, byte[] data) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
