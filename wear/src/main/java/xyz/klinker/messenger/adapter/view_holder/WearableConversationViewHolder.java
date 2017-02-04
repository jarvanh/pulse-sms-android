package xyz.klinker.messenger.adapter.view_holder;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessageListActivity;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;

public class WearableConversationViewHolder extends RecyclerView.ViewHolder {

    public View headerBackground;
    public TextView header;
    public CircleImageView image;
    public TextView name;
    public TextView summary;
    public TextView imageLetter;
    public ImageView groupIcon;
    public View unreadIndicator;
    public CheckBox checkBox;

    public Conversation conversation;
    public int position = -1;

    public WearableConversationViewHolder(final View itemView) {
        super(itemView);

        this.position = -1;

        headerBackground = itemView.findViewById(R.id.header_background);
        header = (TextView) itemView.findViewById(R.id.header);
        image = (CircleImageView) itemView.findViewById(R.id.image);
        name = (TextView) itemView.findViewById(R.id.name);
        summary = (TextView) itemView.findViewById(R.id.summary);
        imageLetter = (TextView) itemView.findViewById(R.id.image_letter);
        groupIcon = (ImageView) itemView.findViewById(R.id.group_icon);
        unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);

        if (conversation != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MessageListActivity.startActivity(itemView.getContext(), conversation.id);
                }
            });
        }
    }


    public boolean isBold() {
        return name.getTypeface() != null && name.getTypeface().isBold();
    }

    public boolean isItalic() {
        return name.getTypeface() != null && name.getTypeface().getStyle() == Typeface.ITALIC;
    }

    public void setTypeface(boolean bold, boolean italic) {
        if (bold) {
            name.setTypeface(Typeface.DEFAULT_BOLD, italic ? Typeface.ITALIC : Typeface.NORMAL);
            summary.setTypeface(Typeface.DEFAULT_BOLD, italic ? Typeface.ITALIC : Typeface.NORMAL);

            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(View.VISIBLE);
            }

            ((CircleImageView) unreadIndicator).setImageDrawable(new ColorDrawable(Settings.get(itemView.getContext()).globalColorSet.color));
        } else {
            name.setTypeface(Typeface.DEFAULT, italic ? Typeface.ITALIC : Typeface.NORMAL);
            summary.setTypeface(Typeface.DEFAULT, italic ? Typeface.ITALIC : Typeface.NORMAL);

            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(View.GONE);
            }
        }
    }
}
