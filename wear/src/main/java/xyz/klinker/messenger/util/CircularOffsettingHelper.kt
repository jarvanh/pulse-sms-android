package xyz.klinker.messenger.util

import android.support.wearable.view.DefaultOffsettingHelper
import android.support.wearable.view.WearableRecyclerView
import android.view.View

class CircularOffsettingHelper : DefaultOffsettingHelper() {
    override fun updateChild(child: View, parent: WearableRecyclerView) {
        val progress = child.top / parent.height
        child.translationX = (-child.height * progress).toFloat()
    }
}
