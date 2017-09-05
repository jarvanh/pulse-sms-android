package xyz.klinker.messenger.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ActivityUtils

class NoLimitMessageListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_full_conversation)

        val conversation = DataSource.getConversation(this, intent.getLongExtra(EXTRA_CONVERSATION_ID, -1))
        if (conversation == null) {
            finish()
            return
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.message_list_container,
                        MessageListFragment.newInstance(conversation, -1, false))
                .commit()

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.title = conversation.title
        toolbar.setBackgroundColor(conversation.colors.color)
        setSupportActionBar(toolbar)

        ActivityUtils.setStatusBarColor(this, conversation.colors.colorDark)
        ActivityUtils.setTaskDescription(this, conversation.title, conversation.colors.color)
    }

    companion object {
        private val EXTRA_CONVERSATION_ID = "conversation_id"

        fun start(context: Context, conversationId: Long) {
            val intent = Intent(context, NoLimitMessageListActivity::class.java)
            intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)

            context.startActivity(intent)
        }
    }
}