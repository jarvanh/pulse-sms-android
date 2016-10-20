package xyz.klinker.messenger.view;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import xyz.klinker.messenger.data.MimeType;

public class ImageKeyboardEditText extends EditText {

    private InputConnectionCompat.OnCommitContentListener commitContentListener;

    public ImageKeyboardEditText(Context context) {
        super(context);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCommitContentListener(InputConnectionCompat.OnCommitContentListener listener) {
        this.commitContentListener = listener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection con = super.onCreateInputConnection(outAttrs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            outAttrs.contentMimeTypes = new String[]{ MimeType.IMAGE_GIF, MimeType.IMAGE_JPEG,
                    MimeType.IMAGE_JPG, MimeType.IMAGE_PNG, MimeType.VIDEO_MP4 };
        }

        return InputConnectionCompat.createWrapper(con, outAttrs, new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
                if (commitContentListener != null) {
                    return commitContentListener.onCommitContent(inputContentInfo, flags, opts);
                } else {
                    return false;
                }
            }
        });
    }

}
