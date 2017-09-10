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

package xyz.klinker.messenger.adapter;

import android.app.Activity;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.util.xml.ChangelogParser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChangelogAdapterTest extends MessengerRobolectricSuite {

    private ChangelogAdapter adapter;

    @Before
    public void setUp() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        adapter = new ChangelogAdapter(activity, ChangelogParser.parse(activity));
    }

    @Test
    public void getCount() {
        assertTrue(adapter.getCount() > 0);
    }

    @Test
    public void getView() {
        TextView textView = (TextView) adapter.getView(0, null, null);
        assertNotNull(textView.getText());
    }

}