package xyz.klinker.messenger.adapter

import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import java.io.File

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener
import xyz.klinker.messenger.shared.util.listener.MediaSelectedListener

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
class MediaGridAdapter(private val mediaMessages: List<Message>, private val callback: MediaSelectedListener?) : RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attach_image, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = Uri.parse(mediaMessages[position].data)
        holder.image.setOnClickListener {
            callback?.onSelected(mediaMessages, holder.adapterPosition)
        }

        holder.image.setBackgroundColor(Color.TRANSPARENT)
        Glide.with(holder.image.context)
                .load(image)
                .apply(RequestOptions().centerCrop())
                .into(holder.image)
    }

    override fun getItemCount() = mediaMessages.size
}
