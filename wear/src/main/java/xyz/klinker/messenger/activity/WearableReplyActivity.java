package xyz.klinker.messenger.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;
import android.widget.ScrollView;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.TextAdapter;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.util.ItemClickListener;

public class WearableReplyActivity extends Activity implements ItemClickListener {

    private static final String RESULT_TEXT = "result_text";
    private static final int ACTIVITY_REQUEST_CODE = 0;
    private static final int VOICE_REQUEST_CODE = 1;
    private static final int TEXT_REQUEST_CODE = 2;

    public static void start(Fragment context) {
        start(context.getActivity());
    }

    public static void start(Activity context) {
        Intent intent = new Intent(context, WearableReplyActivity.class);
        context.startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
    }

    public static String getResultText(Intent data) {
        if (data != null) {
            return data.getStringExtra(RESULT_TEXT);
        } else {
            return null;
        }
    }

    private View voiceReply;
    private View textReply;
    private ScrollView scrollView;
    private WearableRecyclerView recyclerView;

    private TextAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reply);

        voiceReply = findViewById(R.id.voice);
        textReply = findViewById(R.id.text);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        recyclerView = (WearableRecyclerView) findViewById(R.id.recycler_view);

        voiceReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySpeechRecognizer();
            }
        });

        textReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        adapter = new TextAdapter(getResources().getStringArray(R.array.reply_choices), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.getLayoutParams().height = (int) getResources().getDimension(R.dimen.text_height) *
                adapter.getItemCount();

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0,0);
            }
        });
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, VOICE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        String text = null;

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            text = results.get(0);
        } else if (requestCode == TEXT_REQUEST_CODE && resultCode == RESULT_OK) {
            text = "test text";
        }

        finishWithResult(text);
    }

    private void finishWithResult(CharSequence text) {
        Intent result = new Intent();
        result.putExtra(RESULT_TEXT, text);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public void onItemClick(int position) {
        finishWithResult(adapter.getText(position));
    }
}
