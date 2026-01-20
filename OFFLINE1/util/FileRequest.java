package util;
import java.io.Serializable;

public class FileRequest implements Serializable {
    private String fileDescription;
    private String requestingClient;
    private String receivingClient;
    private int reqCount;

    public FileRequest(String fileDescription, String requestingClient, String receivingClient, int reqCount) {
        this.fileDescription = fileDescription;
        this.requestingClient = requestingClient;
        this.receivingClient = receivingClient;
        this.reqCount = reqCount;
    }

    public synchronized void decrementReqCount() {
        if (reqCount > 0) {
            reqCount--;
        }
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public String getRequestingClient() {
        return requestingClient;
    }

    public String getReceivingClient() {
        return receivingClient;
    }
    public int getReqCount() {
        return reqCount;
    }

}