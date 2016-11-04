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

package xyz.klinker.messenger.receiver;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;

public class MmsReceivedReceiverTest extends MessengerRobolectricSuite {

    private MmsReceivedReceiver receiver;
    
    @Mock
    private Context context;

    @Before
    public void setUp() {
        receiver = Mockito.spy(new MmsReceivedReceiver());
        Mockito.doReturn("Test name").when(receiver).getMyName(context);
        Mockito.doReturn("aaa").when(receiver).getContactName(Mockito.eq(context), Mockito.anyString());
    }

    @Test
    public void regularGetPhoneNumbers() {
        assertEquals("+15159911493",
                receiver.getPhoneNumbers("+15159911493", "+15154224558", "5154224558", context));

        assertEquals("+15159911493",
                receiver.getPhoneNumbers("+15159911493", "5154224558", "+15154224558", context));

        assertEquals("5159911493",
                receiver.getPhoneNumbers("5159911493", "5154224558", "5154224558", context));

        assertEquals("+15159911493",
                receiver.getPhoneNumbers("+15159911493", "+15154224558", "+15154224558", context));
    }

    @Test
    public void groupGetPhoneNumbers() {
        assertEquals("5154808532, +15159911493",
                receiver.getPhoneNumbers("+15159911493", "+15154224558, 5154808532", "5154224558", context));

        assertEquals("+15154808532, +15159911493",
                receiver.getPhoneNumbers("+15159911493", "+15154808532, 5154224558", "+15154224558", context));

        assertEquals("+15154808532, +15154196726, +15159911493",
                receiver.getPhoneNumbers("+15159911493", "+15154808532, 5154224558, +15154196726", "+15154224558", context));

    }

}