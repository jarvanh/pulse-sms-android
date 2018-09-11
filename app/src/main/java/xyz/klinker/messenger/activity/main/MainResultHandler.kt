package xyz.klinker.messenger.activity.main

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.support.v4.app.Fragment
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.fragment.PrivateConversationListFragment
import xyz.klinker.messenger.fragment.message.attach.AttachmentListener
import xyz.klinker.messenger.shared.activity.PasscodeVerificationActivity
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.TimeUtils

class MainResultHandler(private val activity: MessengerActivity) {

    fun handle(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PasscodeVerificationActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                activity.navController.conversationActionDelegate.displayFragmentWithBackStack(PrivateConversationListFragment())
                Settings.setValue(activity, activity.getString(R.string.pref_private_conversation_passcode_last_entry), TimeUtils.now)
            } else {
                activity.navController.onNavigationItemSelected(R.id.drawer_conversation)
            }

            return
        }

        var fragment: Fragment? = activity.supportFragmentManager.findFragmentById(R.id.message_list_container)
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data)
        } else {
            if (requestCode == AttachmentListener.RESULT_CAPTURE_IMAGE_REQUEST) {
                Handler().postDelayed({
                    val messageList = activity.supportFragmentManager.findFragmentById(R.id.message_list_container)
                    messageList?.onActivityResult(requestCode, resultCode, data)
                }, 1000)
            }

            fragment = activity.supportFragmentManager.findFragmentById(R.id.conversation_list_container)
            fragment?.onActivityResult(requestCode, resultCode, data)
        }
    }
}