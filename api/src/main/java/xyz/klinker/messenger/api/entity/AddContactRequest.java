package xyz.klinker.messenger.api.entity;

public class AddContactRequest {

    public String accountId;
    public ContactBody[] contacts;

    public AddContactRequest(String accountId, ContactBody[] conversations) {
        this.accountId = accountId;
        this.contacts = conversations;
    }

    public AddContactRequest(String accountId, ContactBody conversation) {
        this.accountId = accountId;
        this.contacts = new ContactBody[1];
        this.contacts[0] = conversation;
    }

}
