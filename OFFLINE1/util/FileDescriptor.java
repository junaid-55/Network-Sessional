package util;
import java.io.Serializable;
public class FileDescriptor implements Serializable {
    private String fileName;
    private long fileSize;
    private boolean isPublic;
    private boolean isRequested;
    private Long requestId;
    
    
    public FileDescriptor(String fileName, long fileSize, boolean isPublic) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isPublic = isPublic;
        this.isRequested = false;
        this.requestId = null;
    }

    public FileDescriptor(String fileName, long fileSize, boolean isRequested, Long requestId) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isPublic = true;
        this.isRequested = isRequested;
        this.requestId = requestId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }

    public boolean isPublic() {
        return isPublic;
    }
    public boolean isRequested() {
        return isRequested;
    }
    public Long getRequestId() {
        return requestId;
    }
}