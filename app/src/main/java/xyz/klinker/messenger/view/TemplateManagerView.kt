package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.TemplateAdapter
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Template
import xyz.klinker.messenger.shared.util.listener.TemplateClickListener
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener

@SuppressLint("ViewConstructor")
class TemplateManagerView(context: Context, colorAccent: Int, private val listener: TextSelectedListener)
    : FrameLayout(context), TemplateClickListener {

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_template_manager, this, true)
        val createTemplate = findViewById<FloatingActionButton>(R.id.create_template)

        createTemplate.backgroundTintList = ColorStateList.valueOf(colorAccent)
        createTemplate.setOnClickListener { createTemplate() }

        loadTemplates()
    }

    private fun createTemplate() {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<EditText>(R.id.edit_text)

        editText.setHint(R.string.type_template_text)

        AlertDialog.Builder(context)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val template = Template()
                    template.text = editText.text.toString()
                    DataSource.insertTemplate(context, template)

                    loadTemplates()
                }.show()
    }

    private fun loadTemplates() {
        val templateList = findViewById<RecyclerView>(R.id.recycler_view)
        val placeholder = findViewById<View>(R.id.placeholder)

        val templates = DataSource.getTemplatesAsList(context)
        val adapter = TemplateAdapter(templates, this)

        templateList.layoutManager = LinearLayoutManager(context)
        templateList.adapter = adapter

        if (templates.isEmpty()) {
            templateList.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
        } else {
            templateList.visibility = View.VISIBLE
            placeholder.visibility = View.GONE
        }
    }

    override fun onClick(template: Template) {
        listener.onTextSelected(template.text!!)
    }

    override fun onLongClick(template: Template) {
        AlertDialog.Builder(context)
                .setMessage(R.string.delete_template)
                .setNegativeButton(R.string.api_no) { _, _ -> }
                .setPositiveButton(R.string.api_yes) { _, _ ->
                    DataSource.deleteTemplate(context, template.id, true)
                    loadTemplates()
                }.show()
    }
}
