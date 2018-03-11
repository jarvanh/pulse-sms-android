package xyz.klinker.messenger.api.implementation

interface AccountInvalidator {

    fun onAccountInvalidated(account: Account)

}