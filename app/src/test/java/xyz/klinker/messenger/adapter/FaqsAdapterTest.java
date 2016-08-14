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
import xyz.klinker.messenger.util.xml.FaqsParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FaqsAdapterTest extends MessengerRobolectricSuite {

    private FaqsAdapter adapter;

    @Before
    public void setUp() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        adapter = new FaqsAdapter(activity, FaqsParser.parse(activity));
    }

    @Test
    public void getCount() {
        assertTrue(adapter.getCount() > 0);
    }

    @Test
    public void getView() {
        View view = adapter.getView(1, null, null);
        TextView question = (TextView) view.findViewById(R.id.question);
        TextView answer = (TextView) view.findViewById(R.id.answer);

        assertNotNull(question.getText());
        assertNotNull(answer.getText());
        assertEquals(View.GONE, answer.getVisibility());

        view.performClick();
        assertEquals(View.VISIBLE, answer.getVisibility());

        view.performClick();
        assertEquals(View.GONE, answer.getVisibility());
    }

}