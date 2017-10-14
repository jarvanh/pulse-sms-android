package xyz.klinker.messenger.shared.util;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

public class AudioWrapperTest extends MessengerRobolectricSuite {

    private Context context = spy(RuntimeEnvironment.application);

    @Mock
    private UiModeManager manager;

    @Before
    public void setUp() {
        when(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(manager);
    }

    @Test
    public void shouldWorkOnAndroidDevice() {
        when(manager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_NORMAL);
        assertThat(AudioWrapper.Companion.shouldPlaySound(context), is(true));
    }

    @Test
    public void shouldNotWorkOnNonTouchscreenDevices() {
        when(manager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_WATCH);
        assertThat(AudioWrapper.Companion.shouldPlaySound(context), is(false));

        when(manager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION);
        assertThat(AudioWrapper.Companion.shouldPlaySound(context), is(false));
    }
}