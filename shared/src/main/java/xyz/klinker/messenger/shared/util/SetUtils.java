package xyz.klinker.messenger.shared.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {

    public static String stringify(Set<String> set) {
        StringBuilder builder = new StringBuilder();

        if (set != null) {
            for (String s : set) {
                builder.append(s);
                builder.append(",");
            }
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    public static Set<String> createSet(String stringified) {
        Set<String> strings = new HashSet<>();

        if (stringified != null && !stringified.isEmpty()) {
            for (String s : stringified.split(",")) {
                strings.add(s);
            }
        }

        return strings;
    }
}
