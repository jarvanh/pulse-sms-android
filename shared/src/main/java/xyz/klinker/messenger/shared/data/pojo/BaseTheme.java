package xyz.klinker.messenger.shared.data.pojo;

public enum BaseTheme {
    DAY_NIGHT(false), ALWAYS_LIGHT(false),
    ALWAYS_DARK(true), BLACK(true);

    public boolean isDark;
    BaseTheme(boolean isDark) {
        this.isDark = isDark;
    }
}