/*
 * Copyright (C) 2017 Luke Klinker
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * Disabling these tests for now due to an issue in robolectric:
 *
 * https://github.com/robolectric/robolectric/issues/2520
 */
@Ignore
public class HelpAndFeedbackFragmentTest extends MessengerRobolectricSuite {

    private HelpAndFeedbackFragment fragment;

    @Before
    public void setUp() {
        fragment = startFragment(new HelpAndFeedbackFragment());
    }

    @Test
    public void isAdded() {
        assertTrue(fragment.isAdded());
    }

    @Test
    public void faqs() {
        fragment.findPreference(fragment.getString(R.string.pref_help_faqs)).performClick();
        assertNotNull(shadowOf(fragment.getActivity()).getNextStartedActivity());
    }

    @Test
    public void features() {
        fragment.findPreference(fragment.getString(R.string.pref_help_features)).performClick();
        assertNotNull(shadowOf(fragment.getActivity()).getNextStartedActivity());
    }

    @Test
    public void email() {
        fragment.findPreference(fragment.getString(R.string.pref_help_email)).performClick();
        assertNotNull(shadowOf(fragment.getActivity()).getNextStartedActivity());
    }

    @Test
    public void twitter() {
        fragment.findPreference(fragment.getString(R.string.pref_help_twitter)).performClick();
        assertNotNull(shadowOf(fragment.getActivity()).getNextStartedActivity());
    }

}