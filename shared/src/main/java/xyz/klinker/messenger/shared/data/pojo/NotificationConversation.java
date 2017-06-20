package xyz.klinker.messenger.shared.data.pojo;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.shared.service.NotificationService;

public class NotificationConversation {
    public long id;
    public long unseenMessageId;
    public String title;
    public String snippet;
    public String imageUri;
    public int color;
    public String ringtoneUri;
    public int ledColor;
    public long timestamp;
    public boolean mute;
    public boolean privateNotification;
    public boolean groupConversation;
    public String phoneNumbers;
    public List<NotificationMessage> messages;

    public NotificationConversation() {
        messages = new ArrayList<>();
    }
}