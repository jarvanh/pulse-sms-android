package xyz.klinker.messenger.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.AttachContactViewHolder;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.listener.AttachContactListener;

public class AttachContactAdapter extends RecyclerView.Adapter<AttachContactViewHolder> {

    private List<Conversation> contacts = new ArrayList<>();
    private AttachContactListener listener;

    public AttachContactAdapter(AttachContactListener listener) {
        setContacts(contacts);
        this.listener = listener;
    }

    @Override
    public AttachContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_attach_contact, parent, false);

        return new AttachContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AttachContactViewHolder holder, int position) {
        final Conversation contact = contacts.get(position);
        holder.getName().setText(contact.getTitle());
        Glide.with(holder.itemView.getContext())
                .load(contact.getImageUri())
                .into(holder.getPicture());

        if (listener != null) {
            holder.getPicture().setOnClickListener(view -> {
                String name = contact.getTitle();
                String phone = contact.getPhoneNumbers();

                String firstName = "";
                String lastName = "";

                if (name.split(" ").length > 1) {
                    firstName = name.split(" ")[0];
                    lastName = name.split(" ")[1];
                }

                listener.onContactAttached(firstName, lastName, phone);
            });
        }
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContacts(List<Conversation> contacts) {
        for (Conversation c : contacts) {
            if (!c.getPhoneNumbers().contains(",") && c.getImageUri() != null && !c.getImageUri().isEmpty()) {
                this.contacts.add(c);
            }
        }

        notifyDataSetChanged();
    }
}
