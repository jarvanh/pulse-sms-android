package xyz.klinker.messenger.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.fragment.message.MessageInstanceManager
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ActivityUtils

class NoLimitMessageListActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_full_conversation)

        val conversation = DataSource.getConversation(this, intent.getLongExtra(EXTRA_CONVERSATION_ID, -1))
        if (conversation == null) {
            finish()
            return
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.message_list_container, MessageInstanceManager.newInstance(conversation, -1, false))
                .commit()

        ActivityUtils.setStatusBarColor(this, conversation.colors.colorDark)
        ActivityUtils.setTaskDescription(this, conversation.title!!, conversation.colors.color)
        MainColorController(this).configureNavigationBarColor()
    }

    companion object {
        private val EXTRA_CONVERSATION_ID = "conversation_id"

        fun start(context: Context?, conversationId: Long) {
            val intent = Intent(context, NoLimitMessageListActivity::class.java)
            intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)

            context?.startActivity(intent)
        }
    }
}
