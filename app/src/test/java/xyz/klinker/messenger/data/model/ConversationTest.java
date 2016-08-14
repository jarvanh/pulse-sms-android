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

package xyz.klinker.messenger.data.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConversationTest {

    private Conversation conversation;

    @Before
    public void setUp() {
        conversation = new Conversation();
    }

    @Test
    public void createStatementNotNull() {
        assertNotNull(conversation.getCreateStatement());
    }

    @Test
    public void indexesNotNull() {
        assertNotNull(conversation.getIndexStatements());
    }

    @Test
    public void tableName() {
        assertEquals("conversation", conversation.getTableName());
    }

    @Test
    public void isGroup() {
        conversation.phoneNumbers = "1, 2, 3";
        assertTrue(conversation.isGroup());
    }

    @Test
    public void isNotGroup() {
        conversation.phoneNumbers = "1";
        assertFalse(conversation.isGroup());
    }

}