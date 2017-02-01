package xyz.klinker.messenger.shared.shared_interfaces;

public interface IConversationListFragment {

    boolean isAdded();
    long getExpandedId();
    IConversationListAdapter getAdapter();

    void checkEmptyViewDisplay();
}
