package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.FrameLayout
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener

@SuppressLint("ViewConstructor")
class TemplateManagerView(context: Context, private val listener: TextSelectedListener, colorAccent: Int) : FrameLayout(context) {

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_template_manager, this, true)

        val templateList = findViewById<RecyclerView>(R.id.recycler_view)
        val createTemplate = findViewById<FloatingActionButton>(R.id.create_template)

        createTemplate.backgroundTintList = ColorStateList.valueOf(colorAccent)
    }
}
