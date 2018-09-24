package xyz.klinker.messenger.shared.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import android.util.AttributeSet

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils

@Suppress("NAME_SHADOWING")
class WhitableToolbar : Toolbar {

    private var appliedBackgroundColor = Integer.MIN_VALUE

    val textColor: Int
        get() = if (appliedBackgroundColor == Integer.MIN_VALUE || ColorUtils.isColorDark(appliedBackgroundColor)) {
            Color.WHITE
        } else {
            resources.getColor(R.color.lightToolbarTextColor)
        }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    override fun setBackgroundColor(color: Int) {
        val color = ActivityUtils.possiblyOverrideColorSelection(context, color)

        super.setBackgroundColor(color)
        this.appliedBackgroundColor = color

        val textColor = textColor
        val tintList = ColorStateList.valueOf(textColor)

        setTitleTextColor(textColor)
        if (overflowIcon != null) overflowIcon!!.setTintList(tintList)
        if (navigationIcon != null) navigationIcon!!.setTintList(tintList)
    }

    override fun setNavigationIcon(res: Int) {
        super.setNavigationIcon(res)

        if (navigationIcon != null) {
            navigationIcon!!.setTintList(ColorStateList.valueOf(textColor))
        }
    }

    override fun inflateMenu(menu: Int) {
        super.inflateMenu(menu)

        (0 until getMenu().size())
                .filter { getMenu().getItem(it).icon != null }
                .forEach { getMenu().getItem(it).icon.setTintList(ColorStateList.valueOf(textColor)) }
    }
}
