package com.sage.app.actions

sealed interface PendingAction {
    val title: String
    val detail: String
    data class Call(val contactName: String, val phoneNumber: String): PendingAction {
        override val title = "Call $contactName?"
        override val detail = phoneNumber
    }
    data class Sms(val contactName: String, val phoneNumber: String, val body: String): PendingAction {
        override val title = "Send message to $contactName?"
        override val detail = "$phoneNumber\n\n$body"
    }
}

sealed interface ActionResult {
    data class Reply(val text: String): ActionResult
    data class Confirmation(val action: PendingAction): ActionResult
}
