package xyz.klinker.messenger.utils.multi_select

import android.os.Bundle
import android.util.SparseBooleanArray
import com.bignerdranch.android.multiselector.SelectableHolder
import java.util.*

open class MultiSelector : com.bignerdranch.android.multiselector.MultiSelector() {
    protected var mSelections = SparseBooleanArray()
    private val mTracker = WeakHolderTracker()
    protected var mIsSelectable: Boolean = false

    /**
     *
     * Current value of selectable.
     *
     * @return True if in selection mode.
     */
    override fun isSelectable(): Boolean {
        return mIsSelectable
    }

    /**
     *
     * Toggle whether this MultiSelector is in selection mode or not.
     * [com.bignerdranch.android.multiselector.SelectableHolder.setSelectable]
     * will be called on any attached holders as well.
     *
     * @param isSelectable True if in selection mode.
     */
    override fun setSelectable(isSelectable: Boolean) {
        mIsSelectable = isSelectable
        refreshAllHolders()
    }

    /**
     *
     * Calls through to [.setSelected].
     *
     * @param holder     Holder to set selection value for.
     * @param isSelected Whether the item should be selected.
     */
    override fun setSelected(holder: SelectableHolder, isSelected: Boolean) {
        setSelected(holder.adapterPosition, holder.itemId, isSelected)
    }

    /**
     *
     * Sets whether a particular item is selected. In this implementation, id is
     * ignored, but subclasses may use id instead.
     *
     * If a holder is bound for this position, this will call through to
     * [com.bignerdranch.android.multiselector.SelectableHolder.setActivated]
     * for that holder.
     *
     * @param position   Position to select/unselect.
     * @param id         Item id to select/unselect. Ignored in this implementation.
     * @param isSelected Whether the item will be selected.
     */
    override fun setSelected(position: Int, id: Long, isSelected: Boolean) {
        mSelections.put(position, isSelected)
        refreshHolder(mTracker.getHolder(position))
    }

    /**
     *
     * Returns whether a particular item is selected.
     *
     * @param position The position to test selection for.
     * @param id       Item id to select/unselect. Ignored in this implementation.
     * @return Whether the item is selected.
     */
    override fun isSelected(position: Int, id: Long): Boolean {
        return mSelections.get(position)
    }

    /**
     *
     * Sets selected to false for all positions. Will refresh
     * all bound holders.
     */
    override fun clearSelections() {
        mSelections.clear()
        refreshAllHolders()
    }

    /**
     *
     * Return a list of selected positions.
     *
     * @return A list of the currently selected positions.
     */
    override fun getSelectedPositions(): List<Int> {
        return (0 until mSelections.size())
                .filter { mSelections.valueAt(it) }
                .map { mSelections.keyAt(it) }
    }

    /**
     *
     * Bind a holder to a specific position/id. This implementation ignores the id.
     *
     * Bound holders will receive calls to [com.bignerdranch.android.multiselector.SelectableHolder.setSelectable]
     * and [com.bignerdranch.android.multiselector.SelectableHolder.setActivated] when
     * [.setSelectable] is called, or when [.setSelected] is called for the
     * associated position, respectively.
     *
     * @param holder   A holder to bind.
     * @param position Position the holder will be bound to.
     * @param id       Item id the holder will be bound to. Ignored in this implementation.
     */
    override fun bindHolder(holder: SelectableHolder, position: Int, id: Long) {
        mTracker.bindHolder(holder, position)
        refreshHolder(holder)
    }

    /**
     *
     * Calls through to [.tapSelection].
     *
     * @param holder The holder to tap.
     * @return True if [.isSelectable] and selection was toggled for this item.
     */
    override fun tapSelection(holder: SelectableHolder): Boolean {
        return tapSelection(holder.adapterPosition, holder.itemId)
    }

    /**
     *
     * Convenience method to ease invoking selection logic.
     * If [.isSelectable] is true, this method toggles selection
     * for the specified item and returns true. Otherwise, it returns false
     * and does nothing.
     *
     * Equivalent to:
     * <pre>
     * `if (multiSelector.isSelectable()) {
     * boolean isSelected = isSelected(position, itemId);
     * setSelected(position, itemId, !isSelected);
     * return true;
     * } else {
     * return false;
     * }
    ` *
    </pre> *
     *
     * @param position Position to tap.
     * @param itemId   Item id to tap. Ignored in this implementation.
     * @return True if the item was toggled.
     */
    override fun tapSelection(position: Int, itemId: Long) =
            if (mIsSelectable) {
                val isSelected = isSelected(position, itemId)
                setSelected(position, itemId, !isSelected)
                true
            } else {
                false
            }

    override fun refreshAllHolders() {
        for (holder in mTracker.trackedHolders) {
            refreshHolder(holder)
        }
    }

    protected open fun refreshHolder(holder: SelectableHolder?) {
        if (holder == null) {
            return
        }
        holder.isSelectable = mIsSelectable

        val isActivated = mSelections.get(holder.adapterPosition)
        holder.isActivated = isActivated
    }


    /**
     * @return Bundle containing the states of the selection and a flag indicating if the multiselection is in
     * selection mode or not
     */

    override fun saveSelectionStates(): Bundle {
        val information = Bundle()
        information.putIntegerArrayList(SELECTION_POSITIONS, selectedPositions as ArrayList<Int>)
        information.putBoolean(SELECTIONS_STATE, isSelectable)
        return information
    }

    /**
     * restore the selection states of the multiselector and the ViewHolder Trackers
     *
     * @param savedStates Saved state bundle, probably from a fragment or activity.
     */
    override fun restoreSelectionStates(savedStates: Bundle?) {
        val selectedPositions = savedStates?.getIntegerArrayList(SELECTION_POSITIONS)
        restoreSelections(selectedPositions)
        mIsSelectable = savedStates?.getBoolean(SELECTIONS_STATE) == true
    }

    private fun restoreSelections(selected: List<Int>?) {
        if (selected == null) return
        var position: Int
        mSelections.clear()
        for (i in selected.indices) {
            position = selected[i]
            mSelections.put(position, true)
        }
        refreshAllHolders()
    }

    companion object {
        private val SELECTION_POSITIONS = "position"
        private val SELECTIONS_STATE = "state"
    }
}