package xyz.klinker.messenger.util;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.InputType;
import android.widget.EditText;

import xyz.klinker.messenger.data.Settings;

public class KeyboardLayoutHelper {

    private Settings.KeyboardLayout layout;

    public KeyboardLayoutHelper(Context context) {
        this(Settings.get(context));
    }

    @VisibleForTesting
    protected KeyboardLayoutHelper(Settings settings) {
        this.layout = settings.keyboardLayout;
    }

    public void applyLayout(EditText editText) {
        int inputType = editText.getInputType();

        switch (layout) {
            case DEFAULT:
                inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                break;
            case ENTER:
                inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                break;
        }

        editText.setInputType(inputType);
    }
}
