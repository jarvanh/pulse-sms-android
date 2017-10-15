package xyz.klinker.messenger.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.AttachContactViewHolder
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.listener.AttachContactListener
import java.util.*

class AttachContactAdapter(private val listener: AttachContactListener?) : RecyclerView.Adapter<AttachContactViewHolder>() {

    private val contacts = ArrayList<Conversation>()

    init {
        setContacts(contacts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachContactViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attach_contact, parent, false)

        return AttachContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttachContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.title
        Glide.with(holder.itemView.context)
                .load(contact.imageUri)
                .into(holder.picture)

        if (listener != null) {
            holder.picture.setOnClickListener {
                val name = contact.title
                val phone = contact.phoneNumbers

                var firstName = ""
                var lastName = ""

                if (name!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1) {
                    firstName = name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    lastName = name.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }

                listener.onContactAttached(firstName, lastName, phone!!)
            }
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    fun setContacts(contacts: List<Conversation>) {
        contacts.filter { !it.phoneNumbers!!.contains(",") && it.imageUri != null && !it.imageUri!!.isEmpty() }
                .forEach { this.contacts.add(it) }

        notifyDataSetChanged()
    }
}
