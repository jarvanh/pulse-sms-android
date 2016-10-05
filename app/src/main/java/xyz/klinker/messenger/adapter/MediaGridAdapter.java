package xyz.klinker.messenger.adapter;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ImageViewHolder;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;
import xyz.klinker.messenger.util.listener.MediaSelectedListener;

/**
 * An adapter for displaying images in a grid for the user to select to attach to a message.
 */
public class MediaGridAdapter extends RecyclerView.Adapter<ImageViewHolder> {

    private List<Message> mediaMessages;
    private MediaSelectedListener callback;

    public MediaGridAdapter(List<Message> mediaMessages, MediaSelectedListener callback) {
        this.mediaMessages = mediaMessages;
        this.callback = callback;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attach_image, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ImageViewHolder holder, final int position) {
        Uri image = Uri.parse(mediaMessages.get(position).data);
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null) {
                    callback.onSelected(mediaMessages, position);
                }
            }
        });

        holder.image.setBackgroundColor(Color.TRANSPARENT);
        Glide.with(holder.image.getContext())
                .load(image)
                .centerCrop()
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        if (mediaMessages == null) {
            return 0;
        } else {
            return mediaMessages.size();
        }
    }

}
