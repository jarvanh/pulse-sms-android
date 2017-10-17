package xyz.klinker.messenger.adapter.message

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.shared.data.ArticlePreview
import xyz.klinker.messenger.shared.data.YouTubePreview
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.ImageUtils

class MessageItemBinder(private val adapter: MessageListAdapter) {

    fun staticImage(holder: MessageViewHolder) {
        Glide.with(holder.itemView.context)
                .load(Uri.parse(holder.data))
                .apply(RequestOptions()
                        .override(holder.image!!.maxHeight, holder.image!!.maxHeight)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .fitCenter())
                .into(holder.image)
    }

    fun animatedGif(holder: MessageViewHolder) {
        holder.image?.maxWidth = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.max_gif_width)
        Glide.with(holder.itemView.context)
                .asGif()
                .load(Uri.parse(holder.data))
                .apply(RequestOptions()
                        .override(holder.image!!.maxHeight, holder.image!!.maxHeight)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .fitCenter())
                .into(holder.image)
    }

    fun video(holder: MessageViewHolder, position: Int) {
        val placeholder = if (adapter.getItemViewType(position) != Message.TYPE_RECEIVED) {
            holder.itemView.context.getDrawable(R.drawable.ic_play_sent)
        } else holder.itemView.context.getDrawable(R.drawable.ic_play)

        Glide.with(holder.itemView.context)
                .asBitmap()
                .load(Uri.parse(holder.data))
                .apply(RequestOptions()
                        .error(placeholder)
                        .placeholder(placeholder)
                        .override(holder.image!!.maxHeight, holder.image!!.maxHeight)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .fitCenter())
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                        ImageUtils.overlayBitmap(holder.itemView.context,
                                resource, R.drawable.ic_play)
                        holder.image?.setImageBitmap(resource)
                    }
                })
    }

    fun audio(holder: MessageViewHolder, position: Int) {
        val placeholder = if (adapter.getItemViewType(position) != Message.TYPE_RECEIVED) {
            holder.itemView.context.getDrawable(R.drawable.ic_audio_sent)
        } else holder.itemView.context.getDrawable(R.drawable.ic_audio)

        Glide.with(holder.itemView.context)
                .load(Uri.parse(holder.data))
                .apply(RequestOptions()
                        .error(placeholder)
                        .placeholder(placeholder))
                .into(holder.image)
    }

    fun vCard(holder: MessageViewHolder, position: Int) {
        holder.message?.text = holder.data

        val placeholder = if (adapter.getItemViewType(position) != Message.TYPE_RECEIVED) {
            holder.itemView.context.getDrawable(R.drawable.ic_contacts_sent)
        } else holder.itemView.context.getDrawable(R.drawable.ic_contacts)

        Glide.with(holder.itemView.context)
                .load(Uri.parse(holder.data))
                .apply(RequestOptions()
                        .error(placeholder)
                        .placeholder(placeholder))
                .into(holder.image)
    }

    @SuppressLint("SetTextI18n")
    fun youTube(holder: MessageViewHolder) {
        val preview = YouTubePreview.build(holder.data!!)
        if (preview != null) {
            Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(Uri.parse(preview.thumbnail))
                    .apply(RequestOptions()
                            .override(holder.image!!.maxHeight, holder.image!!.maxHeight)
                            .fitCenter())
                    .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>) {
                            ImageUtils.overlayBitmap(holder.image!!.context,
                                    bitmap, R.drawable.ic_play)
                            holder.clippedImage!!.setImageBitmap(bitmap)
                        }
                    })

            holder.contact?.text = preview.title
            holder.title.text = "YouTube"

            setGone(holder.image)
            setGone(holder.message)
            setVisible(holder.clippedImage)
            setVisible(holder.contact)
            setVisible(holder.title)
        } else {
            setGone(holder.clippedImage)
            setGone(holder.image)
            setGone(holder.message)
            setGone(holder.timestamp)
            setGone(holder.title)
        }
    }

    fun twitter(holder: MessageViewHolder) {

    }

    fun article(holder: MessageViewHolder) {
        val preview = ArticlePreview.build(holder.data!!)
        if (preview != null) {
            Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(Uri.parse(preview.imageUrl))
                    .apply(RequestOptions()
                            .override(holder.image!!.maxHeight, holder.image!!.maxHeight)
                            .fitCenter())
                    .into(holder.clippedImage)

            holder.contact?.text = preview.title
            holder.message?.text = preview.description
            holder.title.text = preview.domain

            setGone(holder.image)
            setVisible(holder.clippedImage)
            setVisible(holder.contact)
            setVisible(holder.message)
            setVisible(holder.title)
        } else {
            setGone(holder.clippedImage)
            setGone(holder.image)
            setGone(holder.message)
            setGone(holder.timestamp)
            setGone(holder.title)
        }
    }

    internal fun setVisible(v: View?) {
        if (v?.visibility != View.VISIBLE) v?.visibility = View.VISIBLE
    }

    internal fun setGone(v: View?) {
        if (v?.visibility != View.GONE) v?.visibility = View.GONE
    }
}