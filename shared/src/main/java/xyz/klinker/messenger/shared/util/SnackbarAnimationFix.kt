package xyz.klinker.messenger.shared.util

import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.accessibility.AccessibilityManager

import java.lang.reflect.Field

object SnackbarAnimationFix {

    fun apply(snackbar: Snackbar) {
        //        try {
        //            Field mAccessibilityManagerField = BaseTransientBottomBar.class.getDeclaredField("mAccessibilityManager");
        //            mAccessibilityManagerField.setAccessible(true);
        //            AccessibilityManager accessibilityManager = (AccessibilityManager) mAccessibilityManagerField.get(snackbar);
        //            Field mIsEnabledField = AccessibilityManager.class.getDeclaredField("mIsEnabled");
        //            mIsEnabledField.setAccessible(true);
        //            mIsEnabledField.setBoolean(accessibilityManager, false);
        //            mAccessibilityManagerField.set(snackbar, accessibilityManager);
        //        } catch (Throwable e) {
        //            Log.d("Snackbar", "Reflection error: " + e.toString());
        //        }
    }
}
