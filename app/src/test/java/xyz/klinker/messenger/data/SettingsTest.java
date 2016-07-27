/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.data;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SettingsTest extends MessengerRobolectricSuite {

    private Settings settings;

    @Before
    public void setUp() {
        settings = spy(Settings.get(RuntimeEnvironment.application));
    }

    @Test
    public void test_create() {
        assertNotNull(settings.getContext());
        assertNotNull(settings.getSharedPrefs());
    }

    @Test
    public void test_forceUpdate() {
        settings.forceUpdate();
        verify(settings).init(any(Context.class));
    }

    @Test
    public void test_setBooleanValue() {
        settings.setValue("test", true);
        verify(settings).init(any(Context.class));
        assertTrue(settings.getSharedPrefs().getBoolean("test", false));
    }

    @Test
    public void test_setIntValue() {
        settings.setValue("test", 1);
        verify(settings).init(any(Context.class));
        assertTrue(settings.getSharedPrefs().getInt("test", 2) == 1);
    }

    @Test
    public void test_setStringValue() {
        settings.setValue("test", "test string");
        verify(settings, atLeastOnce()).init(any(Context.class));
        assertTrue(settings.getSharedPrefs().getString("test", "not test string").equals("test string"));
    }

    @Test
    public void test_setLongValue() {
        settings.setValue("test", 111L);
        verify(settings).init(any(Context.class));
        assertEquals(111L, settings.getSharedPrefs().getLong("test", -1));
    }

    @Test
    public void test_removeKey() {
        settings.setValue("test", "testvalue");
        settings.removeValue("test");
        assertEquals(null, settings.getSharedPrefs().getString("test", null));
    }

    @Test(expected = RuntimeException.class)
    public void test_cantInitializeWithBlankConstructor() {
        Settings settings = new Settings();
    }

}