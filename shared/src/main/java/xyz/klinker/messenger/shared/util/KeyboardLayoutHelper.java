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
        this(Settings.INSTANCE);
    }

    @VisibleForTesting
    KeyboardLayoutHelper(Settings settings) {
        this.layout = settings.getKeyboardLayout();
    }

    public void applyLayout(EditText editText) {
        int inputType = editText.getInputType();
        int imeOptions = editText.getImeOptions();

        switch (layout) {
            case DEFAULT:
                inputType |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                break;
            case SEND:
                // configure this in the ImageKeyboardEditText to fix some issues
                break;
            case ENTER:
                break;
        }

        editText.setInputType(inputType);
        editText.setImeOptions(imeOptions);
    }
}
