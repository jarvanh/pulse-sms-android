package xyz.klinker.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ActivityUtils;

public class NoLimitMessageListActivity extends AppCompatActivity {

    private static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static void start(Context context, long conversationId) {
        Intent intent = new Intent(context, NoLimitMessageListActivity.class);
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId);

        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_full_conversation);

        Conversation conversation = DataSource.INSTANCE.getConversation(
                this, getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1));
        if (conversation == null) {
            finish();
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.message_list_container, MessageListFragment.newInstance(conversation, -1, false))
                .commit();

        ActivityUtils.setStatusBarColor(this, conversation.colors.colorDark);
        ActivityUtils.setTaskDescription(this, conversation.title, conversation.colors.color);
    }

}
