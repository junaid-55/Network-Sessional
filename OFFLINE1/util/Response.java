package util;
import java.io.Serializable;
public class Response implements Serializable {
    private static final long serialVersionUID =1L;
    private String status;
    private Object data;

    public Response(String status)  {
        this.status = status;
    }

    public Response(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
}
