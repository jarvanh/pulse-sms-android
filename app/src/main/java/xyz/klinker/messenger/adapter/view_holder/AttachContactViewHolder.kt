package xyz.klinker.messenger.adapter.view_holder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import xyz.klinker.messenger.R

class AttachContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val picture: ImageView = itemView.findViewById<View>(R.id.picture) as ImageView
    val name: TextView = itemView.findViewById<View>(R.id.name) as TextView
}
