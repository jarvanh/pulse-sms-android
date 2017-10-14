package xyz.klinker.messenger.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.shared.util.SendUtils;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SendUtilsTest extends MessengerSuite {

    private SendUtils utils;

    @Mock
    private TelephonyManager telephonyManager;
    @Mock
    private Context context;

    @Before
    public void setUp() {
        utils = new SendUtils();
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
    }

    @Test
    public void shouldSplit() {
        when(telephonyManager.getNetworkOperatorName()).thenReturn("U.S. Cellular");
        assertTrue(utils.shouldSplitMessages(context));
    }

    @Test
    public void shouldNotSplit() {
        when(telephonyManager.getNetworkOperatorName()).thenReturn("project_fi");
        assertFalse(utils.shouldSplitMessages(context));
    }

    @Test
    public void handlesNullOperator() {
        when(telephonyManager.getNetworkOperatorName()).thenReturn(null);
        assertFalse(utils.shouldSplitMessages(context));
    }

    @Test
    public void handlesBlankOperator() {
        when(telephonyManager.getNetworkOperatorName()).thenReturn("");
        assertFalse(utils.shouldSplitMessages(context));
    }
}
