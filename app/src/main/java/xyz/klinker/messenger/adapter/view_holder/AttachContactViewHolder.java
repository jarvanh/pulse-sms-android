package xyz.klinker.messenger.adapter.view_holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.klinker.messenger.R;

public class AttachContactViewHolder extends RecyclerView.ViewHolder {

    public ImageView picture;
    public TextView name;

    public AttachContactViewHolder(View itemView) {
        super(itemView);

        name = (TextView) itemView.findViewById(R.id.name);
        picture = (ImageView) itemView.findViewById(R.id.picture);
    }

}
