package xyz.klinker.messenger.api.entity;

public class ContactBody {

    public String phoneNumber;
    public String idMatcher;
    public String name;
    public int color;
    public int colorDark;
    public int colorLight;
    public int colorAccent;

    public ContactBody(String phoneNumber, String idMatcher, String name, int color, int colorDark,
                            int colorLight, int colorAccent) {
        this.phoneNumber = phoneNumber;
        this.idMatcher = idMatcher;
        this.name = name;
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
    }

    @Override
    public String toString() {
        return phoneNumber + ", " + idMatcher + ", " + name + ", " + color + ", " + colorDark + ", " +
                colorLight + ", " + colorAccent;
    }

}
