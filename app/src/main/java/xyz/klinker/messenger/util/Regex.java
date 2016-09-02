package xyz.klinker.messenger.util;

import android.util.Patterns;

import java.util.regex.Pattern;

public class Regex {

    public static final Pattern PHONE = Pattern.compile(
            "(\\+[0-9]+[\\- \\.]*)?"
                    + "(\\([0-9]+\\)[\\- \\.]*)?"
                    + "([0-9][0-9\\- \\.]+[0-9]{3,})");

    public static final Pattern WEB_URL = Patterns.WEB_URL;
    
}
