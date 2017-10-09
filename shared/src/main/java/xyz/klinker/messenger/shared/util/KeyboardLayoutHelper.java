package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout;

public class KeyboardLayoutHelper {

    private KeyboardLayout layout;

    public KeyboardLayoutHelper(Context context) {
        this(Settings.get(context));
    }

    @VisibleForTesting
    KeyboardLayoutHelper(Settings settings) {
        this.layout = settings.keyboardLayout;
    }

    public void applyLayout(EditText editText) {
        int inputType = editText.getInputType();
        int imeOptions = editText.getImeOptions();

        switch (layout) {
            case DEFAULT:
                inputType |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                break;
            case SEND:
                imeOptions |= EditorInfo.IME_ACTION_SEND;
                inputType &= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            case ENTER:
                break;
        }

        editText.setInputType(inputType);
        editText.setImeOptions(imeOptions);
    }
}
