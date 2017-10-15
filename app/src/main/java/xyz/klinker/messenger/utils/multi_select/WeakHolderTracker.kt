package xyz.klinker.messenger.utils.multi_select

import android.util.SparseArray

import com.bignerdranch.android.multiselector.SelectableHolder

import java.lang.ref.WeakReference
import java.util.ArrayList

internal class WeakHolderTracker {
    private val mHoldersByPosition = SparseArray<WeakReference<SelectableHolder>>()

    val trackedHolders: List<SelectableHolder>
        get() = (0 until mHoldersByPosition.size())
                    .map { mHoldersByPosition.keyAt(it) }
                    .mapNotNull { getHolder(it) }

    /**
     * Returns the holder with a given position. If non-null, the returned
     * holder is guaranteed to have getPosition() == position.
     *
     * @param position
     * @return
     */
    fun getHolder(position: Int): SelectableHolder? {
        val holderRef = mHoldersByPosition.get(position) ?: return null

        val holder = holderRef.get()
        if (holder == null || holder.adapterPosition != position) {
            mHoldersByPosition.remove(position)
            return null
        }

        return holder
    }

    fun bindHolder(holder: SelectableHolder, position: Int) {
        mHoldersByPosition.put(position, WeakReference(holder))
    }
}