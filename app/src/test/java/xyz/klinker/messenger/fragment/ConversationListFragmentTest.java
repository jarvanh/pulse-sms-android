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

package xyz.klinker.messenger.fragment;

import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConversationListFragmentTest extends MessengerRobolectricSuite {

    private ConversationListFragment fragment;

    @Before
    public void setUp() {
        fragment = startFragment(ConversationListFragment.Companion.newInstance(-1, -1));
    }

    @Test
    public void notNull() {
        assertNotNull(fragment);
    }

    @Test
    public void isAdded() {
        assertTrue(fragment.isAdded());
    }

}