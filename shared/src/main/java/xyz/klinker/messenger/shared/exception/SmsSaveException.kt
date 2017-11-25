package xyz.klinker.messenger.shared.exception

class SmsSaveException(exception: Exception) : IllegalStateException(exception.message)