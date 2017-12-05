/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.fragment.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

import com.sgottard.sofa.ContentFragment

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerTvActivity
import xyz.klinker.messenger.fragment.message.attach.AttachmentInitializer
import xyz.klinker.messenger.fragment.message.attach.AttachmentListener
import xyz.klinker.messenger.fragment.message.attach.AttachmentManager
import xyz.klinker.messenger.fragment.message.DraftManager
import xyz.klinker.messenger.fragment.message.send.MessageCounterCalculator
import xyz.klinker.messenger.fragment.message.MessageInstanceManager
import xyz.klinker.messenger.fragment.message.load.MessageListLoader
import xyz.klinker.messenger.fragment.message.MessageListNotificationManager
import xyz.klinker.messenger.fragment.message.send.PermissionHelper
import xyz.klinker.messenger.fragment.message.send.SendMessageManager
import xyz.klinker.messenger.fragment.message.load.ViewInitializerDeferred
import xyz.klinker.messenger.fragment.message.load.ViewInitializerNonDeferred
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment
import xyz.klinker.messenger.shared.util.AnimationUtils
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.utils.multi_select.MessageMultiSelectDelegate

/**
 * Fragment for displaying messages for a certain conversation.
 */
class MessageListFragment : Fragment(), ContentFragment, IMessageListFragment {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    val argManager: MessageInstanceManager by lazy { MessageInstanceManager(this) }
    val attachManager: AttachmentManager by lazy { AttachmentManager(this) }
    val attachInitializer: AttachmentInitializer by lazy { AttachmentInitializer(this) }
    val attachListener: AttachmentListener by lazy { AttachmentListener(this) }
    val draftManager: DraftManager by lazy { DraftManager(this) }
    val counterCalculator: MessageCounterCalculator by lazy { MessageCounterCalculator(this) }
    val sendManager: SendMessageManager by lazy { SendMessageManager(this) }
    val messageLoader: MessageListLoader by lazy { MessageListLoader(this) }
    val notificationManager: MessageListNotificationManager by lazy { MessageListNotificationManager(this) }
    private val permissionHelper = PermissionHelper(this)
    private val nonDeferredInitializer: ViewInitializerNonDeferred by lazy { ViewInitializerNonDeferred(this) }
    private val deferredInitializer: ViewInitializerDeferred by lazy { ViewInitializerDeferred(this) }
    val multiSelect: MessageMultiSelectDelegate by lazy { MessageMultiSelectDelegate(this) }

    var rootView: View? = null

    private var updatedReceiver: MessageListUpdatedReceiver? = null
    private var detailsChoiceDialog: AlertDialog? = null

    private var extraMarginTop = 0
    private var extraMarginLeft = 0

    override val conversationId: Long
        get() = argManager.conversationId

    val isDragging: Boolean
        get() = deferredInitializer.dragDismissFrameLayout.isDragging
    val isRecyclerScrolling: Boolean
        get() = messageLoader.messageList.scrollState != RecyclerView.SCROLL_STATE_IDLE

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_message_list, parent, false)

        if (!isAdded) {
            return rootView!!
        }

        nonDeferredInitializer.init(bundle)

        AnimationUtils.animateConversationPeripheralIn(rootView!!.findViewById(R.id.app_bar_layout))
        AnimationUtils.animateConversationPeripheralIn(rootView!!.findViewById(R.id.send_bar))

        val deferredTime = if (activity is MessengerTvActivity) 0L
        else (AnimationUtils.EXPAND_CONVERSATION_DURATION + 25).toLong()

        Handler().postDelayed({
            if (!isAdded) {
                return@postDelayed
            }

            deferredInitializer.init()
            messageLoader.loadMessages()

            notificationManager.dismissNotification = true
            notificationManager.dismissNotification()
        }, deferredTime)

        return rootView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatedReceiver = MessageListUpdatedReceiver(this)
        fragmentActivity?.registerReceiver(updatedReceiver,
                MessageListUpdatedReceiver.intentFilter)

        if (extraMarginLeft != 0 || extraMarginTop != 0) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = extraMarginLeft
            view.invalidate()
        }
    }

    override fun onStart() {
        super.onStart()
        notificationManager.onStart()

        Handler().postDelayed({
            if (fragmentActivity != null) Thread { DataSource.readConversation(fragmentActivity!!, conversationId) }.start()
        }, (AnimationUtils.EXPAND_CONVERSATION_DURATION + 50).toLong())
    }

    override fun onResume() {
        super.onResume()
        NotificationConstants.CONVERSATION_ID_OPEN = conversationId

    }

    override fun onPause() {
        super.onPause()
        NotificationConstants.CONVERSATION_ID_OPEN = 0L
    }

    override fun onStop() {
        super.onStop()
        notificationManager.dismissNotification = false

        Handler().postDelayed({
            if (fragmentActivity != null) Thread { DataSource.readConversation(fragmentActivity!!, conversationId) }.start()
        }, (AnimationUtils.EXPAND_CONVERSATION_DURATION + 50).toLong())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        draftManager.createDrafts()
        sendManager.sendOnFragmentDestroyed()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (updatedReceiver != null) {
            fragmentActivity?.unregisterReceiver(updatedReceiver)
            updatedReceiver = null
        }

        draftManager.createDrafts()
        multiSelect.clearActionMode()
    }

    override fun onDetach() {
        super.onDetach()
        CursorUtil.closeSilent(messageLoader.adapter?.messages)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (!permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        attachListener.onActivityResult(requestCode, resultCode, data)
    }

    override fun getFocusRootView() = try {
            messageLoader.messageList.getChildAt(messageLoader.messageList.childCount - 1)
        } catch (e: NullPointerException) { null }

    override fun setExtraMargin(marginTop: Int, marginLeft: Int) {
        this.extraMarginTop = marginTop
        this.extraMarginLeft = marginLeft
    }

    override fun isScrolling() = false
    override fun setConversationUpdateInfo(text: String) { messageLoader.informationUpdater.setConversationUpdateInfo(text) }
    override fun setDismissOnStartup() { notificationManager.dismissOnStartup = true }
    override fun setShouldPullDrafts(pull: Boolean) { draftManager.pullDrafts = pull }
    override fun loadMessages() { messageLoader.loadMessages(false) }
    override fun loadMessages(addedNewMessage: Boolean) { messageLoader.loadMessages(addedNewMessage) }
    fun resendMessage(originalMessageId: Long, text: String) { sendManager.resendMessage(originalMessageId, text) }
    fun setDetailsChoiceDialog(dialog: AlertDialog) { this.detailsChoiceDialog = dialog }

    fun onBackPressed(): Boolean {
        dismissDetailsChoiceDialog()

        if (attachManager.backPressed()) {
            return true
        }

        sendManager.sendDelayedMessage()
        if (updatedReceiver != null) {
            fragmentActivity?.unregisterReceiver(updatedReceiver)
            updatedReceiver = null
        }

        return false
    }

    fun dismissKeyboard() {
        try {
            val imm = fragmentActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(fragmentActivity?.findViewById<View>(android.R.id.content)?.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun dismissDetailsChoiceDialog() {
        if (detailsChoiceDialog != null && detailsChoiceDialog!!.isShowing) {
            detailsChoiceDialog!!.dismiss()
            detailsChoiceDialog = null
        }
    }
}
