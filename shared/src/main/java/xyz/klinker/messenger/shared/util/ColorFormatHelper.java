package xyz.klinker.messenger.shared.util;

public class ColorFormatHelper {

    private static int assertColorValueInRange(int colorValue) {
        return ((0 <= colorValue) && (colorValue <= 255)) ? colorValue : 0;
    }

    public static String formatColorValues(
            int red, int green, int blue) {

        return String.format("%02X%02X%02X",
                assertColorValueInRange(red),
                assertColorValueInRange(green),
                assertColorValueInRange(blue)
        );
    }

    public static String formatColorValues(int alpha, int red, int green, int blue) {

        return String.format("%02X%02X%02X%02X",
                assertColorValueInRange(alpha),
                assertColorValueInRange(red),
                assertColorValueInRange(green),
                assertColorValueInRange(blue)
        );
    }

}