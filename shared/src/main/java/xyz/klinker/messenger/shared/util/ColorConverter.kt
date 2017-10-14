package xyz.klinker.messenger.shared.util

import android.graphics.Color

/**
 * Class used to help with the Material Design Color palette, which is created
 * by lightening and darkening base colors.
 */
object ColorConverter {

    /**
     * When converting a material design primary color, to its darker version, we darken by 12.
     *
     * @param color the primary color.
     * @return the darker version of the primary color.
     */
    fun darkenPrimaryColor(color: Int): Int {
        return darken(color, 12)
    }

    /**
     * When converting a material design primary color, to its lighter version, we lighten by 12.
     *
     * @param color the primary color.
     * @return the darker version of the primary color.
     */
    fun lightenPrimaryColor(color: Int): Int {
        return if (color == Color.WHITE) {
            Color.WHITE
        } else if (color == Color.BLACK) {
            Color.BLACK
        } else {
            lighten(color, 10)
        }
    }

    /**
     * Darkens a given color.
     *
     * @param base base color
     * @param amount amount between 0 and 100
     * @return darken color
     */
    private fun darken(base: Int, amount: Int): Int {
        var hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)

        val hsl = hsv2hsl(hsv)
        hsl[2] -= amount / 100f

        if (hsl[2] < 0)
            hsl[2] = 0f

        hsv = hsl2hsv(hsl)
        return Color.HSVToColor(hsv)
    }

    /**
     * lightens a given color
     * @param base base color
     * @param amount amount between 0 and 100
     * @return lightened
     */
    fun lighten(base: Int, amount: Int): Int {
        var hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)

        val hsl = hsv2hsl(hsv)
        hsl[2] += amount / 100f

        if (hsl[2] > 1)
            hsl[2] = 1f

        hsv = hsl2hsv(hsl)
        return Color.HSVToColor(hsv)
    }

    /**
     * Converts HSV (Hue, Saturation, Value) color to HSL (Hue, Saturation, Lightness)
     * https://gist.github.com/xpansive/1337890
     *
     * @param hsv HSV color array
     * @return hsl
     */
    private fun hsv2hsl(hsv: FloatArray): FloatArray {
        val hue = hsv[0]
        val sat = hsv[1]
        val `val` = hsv[2]

        val nhue = (2f - sat) * `val`
        var nsat = sat * `val` / if (nhue < 1f) nhue else 2f - nhue
        if (nsat > 1f)
            nsat = 1f

        return floatArrayOf(hue, nsat, nhue / 2f)
    }

    /**
     * Reverses hsv2hsl
     * https://gist.github.com/xpansive/1337890
     *
     * @param hsl HSL color array
     * @return hsv color array
     */
    private fun hsl2hsv(hsl: FloatArray): FloatArray {
        val hue = hsl[0]
        var sat = hsl[1]
        val light = hsl[2]

        sat *= if (light < .5) light else 1 - light

        return floatArrayOf(hue, 2f * sat / (light + sat), light + sat)
    }
}