package xyz.klinker.messenger.api.entity;

public class UpdateContactRequest {

    public String phoneNumber;
    public String name;
    public Integer color;
    public Integer colorDark;
    public Integer colorLight;
    public Integer colorAccent;

    public UpdateContactRequest(String phoneNumber, String name, Integer color, Integer colorDark,
                                    Integer colorLight, Integer colorAccent) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
    }
}
