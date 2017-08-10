package xyz.klinker.messenger.view;

import android.content.Context;
import android.os.Bundle;
import android.support.text.emoji.widget.EmojiEditTextHelper;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.os.BuildCompat;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

public class ImageKeyboardEditText extends EditText {

    private EmojiEditTextHelper emojiHelper;
    private InputConnectionCompat.OnCommitContentListener commitContentListener;

    public ImageKeyboardEditText(Context context) {
        this(context, null);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setKeyListener(getEmojiHelper().getKeyListener(getKeyListener()));
    }

    public void setCommitContentListener(InputConnectionCompat.OnCommitContentListener listener) {
        this.commitContentListener = listener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection con = super.onCreateInputConnection(outAttrs);
        EditorInfoCompat.setContentMimeTypes(outAttrs, new String[] { "image/gif", "image/png" });
        con = InputConnectionCompat.createWrapper(con, outAttrs, (inputContentInfo, flags, opts) -> {
            if (commitContentListener != null) {
                if (BuildCompat.isAtLeastNMR1() &&
                        (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                    try {
                        inputContentInfo.requestPermission();
                    } catch (Exception e) {
                        return false;
                    }
                }

                commitContentListener.onCommitContent(
                        inputContentInfo, flags, opts
                );

                return true;
            } else {
                return false;
            }
        });

        return getEmojiHelper().onCreateInputConnection(con, outAttrs);
    }

    @Override
    public void setKeyListener(android.text.method.KeyListener keyListener) {
        super.setKeyListener(getEmojiHelper().getKeyListener(keyListener));
    }

    private EmojiEditTextHelper getEmojiHelper() {
        if (emojiHelper == null) {
            emojiHelper = new EmojiEditTextHelper(this);
        }

        return emojiHelper;
    }

}
