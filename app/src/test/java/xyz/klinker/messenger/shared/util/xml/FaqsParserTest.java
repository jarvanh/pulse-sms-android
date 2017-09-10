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

package xyz.klinker.messenger.shared.util.xml;

import android.app.Activity;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class FaqsParserTest extends MessengerRobolectricSuite {

    @Before
    public void setUp() {
        FaqsParser utils = new FaqsParser();
    }

    @Test
    public void notNull() {
        String[] items = FaqsParser.parse(RuntimeEnvironment.application);
        assertNotNull(items);
        assertTrue(items.length > 0);
    }

    @Test
    public void nullItems() {
        Activity activity = Mockito.mock(Activity.class);
        Resources resources = Mockito.mock(Resources.class);
        when(resources.getXml(R.xml.faqs)).thenReturn(null);

        String[] items = FaqsParser.parse(activity);
        assertNull(items);
    }

}