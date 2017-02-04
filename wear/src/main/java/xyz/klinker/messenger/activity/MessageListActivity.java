package xyz.klinker.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.view.MenuItem;

import java.util.ArrayList;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.WearableConversationListAdapter;
import xyz.klinker.messenger.adapter.WearableMessageListAdapter;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.util.CircularOffsettingHelper;

public class MessageListActivity extends AppCompatActivity implements IMessageListFragment {

    private static final String CONVERSATION_ID = "conversation_id";

    public static void startActivity(Context context, long conversationId) {
        Intent intent = new Intent(context, MessageListActivity.class);
        intent.putExtra(CONVERSATION_ID, conversationId);

        context.startActivity(intent);
    }

    private DataSource source;
    private Conversation conversation;

    private WearableDrawerLayout drawerLayout;
    private WearableActionDrawer actionDrawer;
    private WearableRecyclerView recyclerView;

    private LinearLayoutManager manager;
    private WearableMessageListAdapter adapter;

    private MessageListUpdatedReceiver updatedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message_list);

        drawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        actionDrawer = (WearableActionDrawer) findViewById(R.id.action_drawer);
        recyclerView = (WearableRecyclerView) findViewById(R.id.recycler_view);

        source = DataSource.getInstance(this);
        source.open();

        conversation = source.getConversation(getIntent().getLongExtra(CONVERSATION_ID, -1L));

        if (conversation == null) {
            finish();
            return;
        }

        initRecycler();
        loadMessages();
        dismissNotification();

        updatedReceiver = new MessageListUpdatedReceiver(this);
        registerReceiver(updatedReceiver,
                MessageListUpdatedReceiver.getIntentFilter());

        actionDrawer.setBackgroundColor(conversation.colors.colorAccent);
        actionDrawer.setOnMenuItemClickListener(new WearableActionDrawer.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(updatedReceiver);

        if (adapter != null) {
            adapter.getMessages().close();
        }

        if (source != null) {
            source.close();
        }
    }

    private void initRecycler() {
        manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);

        adapter = new WearableMessageListAdapter(this, manager, null, conversation.colors.color, conversation.colors.colorAccent);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        recyclerView.setOffsettingHelper(new CircularOffsettingHelper());
    }

    @Override
    public void loadMessages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = source.getMessages(conversation.id);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.getMessages() == null) {
                            adapter.setMessages(cursor);
                        } else {
                            adapter.addMessage(cursor);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public long getConversationId() {
        return conversation.id;
    }

    @Override
    public void setShouldPullDrafts(boolean pull) {

    }

    @Override
    public void setDismissOnStartup() {

    }

    @Override
    public void setConversationUpdateInfo(String text) {

    }

    private void dismissNotification() {
        NotificationManagerCompat.from(this).cancel((int) conversation.id);

        new ApiUtils().dismissNotification(Account.get(this).accountId,
                Account.get(this).deviceId, conversation.id);

        NotificationUtils.cancelGroupedNotificationWithNoContent(this);
    }
}
