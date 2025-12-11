package util;
import java.io.Serializable;
public class Request implements Serializable{
    private static final long seialVersionUID = 1L;
    private String method;
    private String type;
    private Object data;
    private Long fileId;
    public Request(String method, String  type) {
        this.method = method;
        this.type = type;
    }

    public Request(String method, String type, Object data) {
        this.method = method;
        this.type = type;
        this.data = data;
    }

    public Request(String method, String type, Object data, Long fileId) {
        this.method = method;
        this.type = type;
        this.data = data;
        this.fileId = fileId;
    }
    
    public Long getFileId() {
        return fileId;
    }
    public String getMethod() {
        return method;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}