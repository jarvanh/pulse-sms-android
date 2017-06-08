package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.InputType;
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

        switch (layout) {
            case DEFAULT:
                inputType |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                break;
            case SEND:
            case ENTER:
                break;
        }

        editText.setInputType(inputType);
    }
}
