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

package xyz.klinker.messenger.fragment.settings;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Disabling these tests for now due to an issue in robolectric:
 *
 * https://github.com/robolectric/robolectric/issues/2520
 */
@Ignore
public class AboutFragmentTest extends MessengerRobolectricSuite {

    private AboutFragment fragment;

    @Mock
    private FragmentActivity activity;

    @Before
    public void setUp() {
        fragment = startFragment(new AboutFragment());
    }

    @Test
    public void isAdded() {
        assertTrue(fragment.isAdded());
    }

    @Test
    public void versionName() {
        assertNotNull(fragment.getVersionName());
    }

    @Test
    public void nullVersionName() {
        fragment = spy(fragment);
        when(fragment.getActivity()).thenReturn(activity);
        when(activity.getPackageName()).thenReturn(null);

        assertNull(fragment.getVersionName());
    }

    @Test
    public void clickChangelog() {
        fragment = spy(fragment);
        fragment.findPreference(fragment.getString(R.string.pref_about_changelog)).performClick();
    }

    @Test
    public void clickOpenSource() {
        fragment = spy(fragment);
        fragment.findPreference(fragment.getString(R.string.pref_about_copyright)).performClick();
    }

}