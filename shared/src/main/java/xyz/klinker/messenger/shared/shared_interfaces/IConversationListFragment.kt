package xyz.klinker.messenger.shared.shared_interfaces

interface IConversationListFragment {

    val isFragmentAdded: Boolean
    val expandedId: Long
    val adapter: IConversationListAdapter?

    fun checkEmptyViewDisplay()
}
