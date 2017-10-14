package xyz.klinker.messenger.shared.shared_interfaces

interface IConversationListFragment {

    val isAdded: Boolean
    val expandedId: Long
    val adapter: IConversationListAdapter

    fun checkEmptyViewDisplay()
}
