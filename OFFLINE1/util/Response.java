package util;
import java.io.Serializable;
public class Response implements Serializable {
    private String status,type;
    private Object data;


    public Response(String status)  {
        this.status = status;
    }

    public Response(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    public Response(String status, String type, Object data) {
        this.status = status;
        this.type = type;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
    public String getType() {
        return type;
    }
}
