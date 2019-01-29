package xyz.klinker.messenger.api.entity;

public class ContactBody {

    public long deviceId;
    public String phoneNumber;
    public String idMatcher;
    public String name;
    public int contactType;
    public int color;
    public int colorDark;
    public int colorLight;
    public int colorAccent;

    public ContactBody(long id, String phoneNumber, String idMatcher, String name, int color, int colorDark,
                            int colorLight, int colorAccent) {
        this(id, phoneNumber, idMatcher, name, 4, color, colorDark, colorLight, colorAccent);
    }

    public ContactBody(long id, String phoneNumber, String idMatcher, String name, int type, int color, int colorDark,
                            int colorLight, int colorAccent) {
        this.deviceId = id;
        this.phoneNumber = phoneNumber;
        this.idMatcher = idMatcher;
        this.name = name;
        this.contactType = type;
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
    }

    @Override
    public String toString() {
        return deviceId + ", " + phoneNumber + ", " + idMatcher + ", " + name + ", " + contactType + ", " +
                color + ", " + colorDark + ", " + colorLight + ", " + colorAccent;
    }

}
