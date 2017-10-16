package xyz.klinker.messenger.activity.notification

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.service.ReplyService

class MarshmallowReplyActivity : AppCompatActivity() {

    private val animators = ReplyAnimators(this)
    private val dataProvider = ReplyDataProvider(this)
    private val wearableHandler = ReplyWearableHandler(this)
    private val layoutInitializer = ReplyLayoutInitializer(this, dataProvider, animators)
    private val sender = ReplySender(this, dataProvider, animators)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_reply)

        overridePendingTransition(0, 0)

        if (wearableHandler.reply() || dataProvider.conversation == null) {
            finish()
            return
        }

        dataProvider.queryMessageHistory()

        sender.setupViews()
        layoutInitializer.setupBackgroundComponents()
        layoutInitializer.showContactImage()

        animators.alphaIn(findViewById<View>(R.id.dim_background), 300, 0)
        findViewById<View>(android.R.id.content).post({ layoutInitializer.displayMessages() })

        sender.requestFocus()
        dataProvider.dismissNotification()
    }

    override fun onBackPressed() {
        sender.saveDraft()
        animators.slideOut()
        Handler().postDelayed({
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }, 300)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        getIntent().putExtra(ReplyService.EXTRA_CONVERSATION_ID, intent.getLongExtra(ReplyService.EXTRA_CONVERSATION_ID, -1))
        recreate()
    }
}
