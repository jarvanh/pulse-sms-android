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

package xyz.klinker.messenger.encryption;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.crypto.SecretKey;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;

public class EncryptionTest extends MessengerRobolectricSuite {

    private static final String PASSWORD = "@`h?_c#%S5~g>[.Q6}!dVTm?_%Y[':sX";
    private static final String ACCOUNT_ID = "tZjFQXbQxqxPUGTJd8U7xDRSttPXm3Sa";
    private static final String SALT1 = "z78B@~!s";
    private static final String SALT2 = "w4SMmd$F";

    private KeyUtils keyUtils;
    private EncryptionUtils encryptionUtils;

    @Before
    public void setUp() {
        keyUtils = new KeyUtils();
        String hash = keyUtils.hashPassword(PASSWORD, SALT2);
        SecretKey key = keyUtils.createKey(hash, ACCOUNT_ID, SALT1);
        encryptionUtils = new EncryptionUtils(key);
    }

    @Test @Ignore
    public void encryptAndDecrypt() {
        String text = "hey, what's up? Just testing encryption and decryption using " +
                "strong-encryption techniques :)";
        String encrypted = encryptionUtils.encrypt(text);
        String decrypted = encryptionUtils.decrypt(encrypted);
        assertEquals(text, decrypted);
    }

}
