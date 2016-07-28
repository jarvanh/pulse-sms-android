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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.data.model.Conversation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ContactAdapterTest extends MessengerSuite {

    private ContactAdapter adapter;

    @Mock
    private List<Conversation> conversations;

    @Before
    public void setUp() {
        adapter = new ContactAdapter(conversations, null);
    }

    @Test
    public void getSizeZero() {
        when(conversations.size()).thenReturn(0);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void getSize() {
        when(conversations.size()).thenReturn(10);
        assertEquals(10, adapter.getItemCount());
    }

}