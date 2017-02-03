package xyz.klinker.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MessageListActivity extends AppCompatActivity {

    private static final String CONVERSATION_ID = "conversation_id";

    public static void startActivity(Context context, long conversationId) {
        Intent intent = new Intent(context, MessageListActivity.class);
        intent.putExtra(CONVERSATION_ID, conversationId);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
