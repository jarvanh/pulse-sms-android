package xyz.klinker.messenger.shared.util;

import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AudioWrapperTest extends MessengerSuite {

    // these passed fine locally, but the build server didn't like them, for some reason...

    @Mock
    private Context context;
    @Mock
    private UiModeManager uiModeManager;
    @Mock
    private NotificationManager notificationManager;

    @Before
    public void setUp() {
        when(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(uiModeManager);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager);
    }

    @Test
    public void shouldWorkOnAndroidDevice() {
        when(notificationManager.getCurrentInterruptionFilter()).thenReturn(NotificationManager.INTERRUPTION_FILTER_NONE);
        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_NORMAL);
        assertTrue(AudioWrapper.Companion.shouldPlaySound(context, 27));
    }

    @Test
    public void shouldNotSoundWhenDoNoDisturbIsOn() {
        when(notificationManager.getCurrentInterruptionFilter()).thenReturn(NotificationManager.INTERRUPTION_FILTER_ALARMS);
        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_NORMAL);
        assertFalse(AudioWrapper.Companion.shouldPlaySound(context, 27));

        when(notificationManager.getCurrentInterruptionFilter()).thenReturn(NotificationManager.INTERRUPTION_FILTER_ALL);
        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_NORMAL);
        assertFalse(AudioWrapper.Companion.shouldPlaySound(context, 27));
    }

    @Test
    public void shouldNotWorkOnNonTouchscreenDevices() {
        when(notificationManager.getCurrentInterruptionFilter()).thenReturn(NotificationManager.INTERRUPTION_FILTER_NONE);

        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_WATCH);
        assertFalse(AudioWrapper.Companion.shouldPlaySound(context, 27));

        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION);
        assertFalse(AudioWrapper.Companion.shouldPlaySound(context, 27));
    }

}