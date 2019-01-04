package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
        showTemplateDialog { text ->
            val template = Template()
            template.text = text
            DataSource.insertTemplate(context, template)

            loadTemplates()
        }
    }

    private fun showTemplateDialog(currentText: String = "", callback: (value: String) -> Unit) {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null, false)
        val editText = layout.findViewById<EditText>(R.id.edit_text)

        editText.setHint(R.string.type_template_text)
        editText.setText(currentText)

        AlertDialog.Builder(context)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val text = editText.text.toString()
                    callback(text)
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
                .setNegativeButton(R.string.delete) { _, _ ->
                    DataSource.deleteTemplate(context, template.id, true)
                    loadTemplates()
                }.setPositiveButton(R.string.edit) { _, _ ->
                    showTemplateDialog(template.text!!) { text ->
                        template.text = text
                        DataSource.updateTemplate(context, template)

                        loadTemplates()
                    }
                }.show()
    }
}
