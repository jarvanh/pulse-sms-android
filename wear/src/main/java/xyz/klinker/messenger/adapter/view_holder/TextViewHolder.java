package xyz.klinker.messenger.adapter.view_holder;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import xyz.klinker.messenger.R;

public class TextViewHolder extends RecyclerView.ViewHolder {

    public TextView textView;

    public static TextViewHolder create(ViewGroup parent) {
        return create(parent, R.layout.item_text);
    }

    public static TextViewHolder create(ViewGroup parent, @LayoutRes int res) {
        View root = LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new TextViewHolder(root);
    }

    private TextViewHolder(View itemView) {
        super(itemView);

        if (itemView instanceof TextView) {
            textView = (TextView) itemView;
        }
    }
}
