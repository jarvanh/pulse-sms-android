package xyz.klinker.messenger.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.TextViewHolder;
import xyz.klinker.messenger.util.ItemClickListener;

public class TextAdapter extends RecyclerView.Adapter<TextViewHolder> {

    private static final int TYPE_TEXT = 1;
    private static final int TYPE_FOOTER = 2;

    private CharSequence[] texts;
    private ItemClickListener itemClickListener;

    public TextAdapter(@NonNull CharSequence[] texts, ItemClickListener itemClickListener) {
        this.texts = texts;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return TYPE_FOOTER;
        } else {
            return TYPE_TEXT;
        }
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            return TextViewHolder.create(parent, R.layout.item_footer);
        }

        return TextViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(TextViewHolder holder, final int position) {
        if (position == getItemCount() - 1) {
            return;
        }

        holder.textView.setText(getText(position));
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClick(position);
            }
        });
    }

    public CharSequence getText(int position) {
        return texts[position];
    }

    @Override
    public int getItemCount() {
        return texts.length + 1;
    }
}
