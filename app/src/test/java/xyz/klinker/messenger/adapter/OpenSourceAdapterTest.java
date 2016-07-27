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

package xyz.klinker.messenger.adapter;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.xml.OpenSourceParser;

import static org.junit.Assert.*;

public class OpenSourceAdapterTest extends MessengerRobolectricSuite {

    private OpenSourceAdapter adapter;

    @Before
    public void setUp() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        adapter = new OpenSourceAdapter(activity, OpenSourceParser.parse(activity));
    }

    @Test
    public void getCount() {
        assertTrue(adapter.getCount() > 0);
    }

    @Test
    public void getView() {
        View view = adapter.getView(1, null, null);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView license = (TextView) view.findViewById(R.id.license);

        assertNotNull(title.getText());
        assertNotNull(license.getText());
    }

}