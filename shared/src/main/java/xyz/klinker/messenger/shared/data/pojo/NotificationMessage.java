package xyz.klinker.messenger.shared.data.pojo;

public class NotificationMessage {
    public long id;
    public String data;
    public String mimeType;
    public long timestamp;
    public String from;

    public NotificationMessage(long id, String data, String mimeType, long timestamp, String from) {
        this.id = id;
        this.data = data;
        this.mimeType = mimeType;
        this.timestamp = timestamp;
        this.from = from;
    }
}