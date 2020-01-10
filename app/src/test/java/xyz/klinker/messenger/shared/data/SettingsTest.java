/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.data;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SettingsTest extends MessengerRobolectricSuite {

    private Settings settings;
    private Context context = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        settings = spy(Settings.INSTANCE);
        settings.init(context);
    }

    @Test
    public void create() {
        assertNotNull(settings.getSharedPrefs(context));
    }

    @Test
    public void forceUpdate() {
        settings.forceUpdate(context);
        verify(settings, atLeastOnce()).init(any(Context.class));
    }

    @Test
    public void setBooleanValue() {
        settings.setValue(context, "test", true);
        verify(settings, atLeastOnce()).init(any(Context.class));
        assertTrue(settings.getSharedPrefs(context).getBoolean("test", false));
    }

    @Test
    public void setIntValue() {
        settings.setValue(context, "test", 1);
        verify(settings, atLeastOnce()).init(any(Context.class));
        assertTrue(settings.getSharedPrefs(context).getInt("test", 2) == 1);
    }

    @Test
    public void setStringValue() {
        settings.setValue(context, "test", "test string");
        verify(settings, atLeastOnce()).init(any(Context.class));
        assertTrue(settings.getSharedPrefs(context).getString("test", "not test string").equals("test string"));
    }

    @Test
    public void setLongValue() {
        settings.setValue(context, "test", 111L);
        verify(settings, atLeastOnce()).init(any(Context.class));
        assertEquals(111L, settings.getSharedPrefs(context).getLong("test", -1));
    }

    @Test
    public void removeKey() {
        settings.setValue(context, "test", "testvalue");
        settings.removeValue(context, "test");
        assertEquals(null, settings.getSharedPrefs(context).getString("test", null));
    }

}