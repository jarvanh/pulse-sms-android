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

package xyz.klinker.messenger.adapter;

import android.app.Activity;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class ScheduledMessagesAdapterTest extends MessengerRobolectricSuite {

    private ScheduledMessagesAdapter adapter;

    @Before
    public void setUp() {
        adapter = new ScheduledMessagesAdapter(new ArrayList<ScheduledMessage>(), null);
    }

    @Test
    public void getCountZero() {
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void createViewHolder() {
        ViewGroup parent = new LinearLayout(Robolectric.setupActivity(Activity.class));
        assertNotNull(adapter.onCreateViewHolder(parent, 0));
    }

}