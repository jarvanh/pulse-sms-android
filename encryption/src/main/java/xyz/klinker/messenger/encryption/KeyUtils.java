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

package xyz.klinker.messenger.encryption;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilities for creating keys from passwords, hashes, and salts.
 */
public class KeyUtils {

    private static final String TAG = "KeyUtils";

    /**
     * The number of iterations we should preform when hashing the key. Higher means more time but
     * better security.
     * <p>
     * Some stats:
     * 65,000 = 2.2 seconds on Nexus 6P
     * 1000 = 42 ms on Nexus 6P
     * 10000 = 400 ms on Nexus 6P
     */
    private static final int ITERATIONS = 10000;

    /**
     * The length of the key we should generate. Higher means more security but longer computation
     * again.
     */
    private static final int KEY_LENGTH = 256;

    /**
     * Hashes the password provided by the user during the login sequence to something that can be
     * stored in the shared preferences and is encrypted. We can then use this encrypted password
     * hash later with another salt to create our SecretKey which we will encrypt data with before
     * sending it to the server.
     *
     * @param password the password to encode with the given salt.
     * @param salt     the salt to use (should be gotten from the server after a successful login
     *                 request).
     * @return a Base64 encoded secret key.
     */
    public String hashPassword(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8),
                    ITERATIONS, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            return Base64.encodeToString(secret.getEncoded(), Base64.DEFAULT);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            System.out.println("could not create key");
            e.printStackTrace();
        }

        throw new RuntimeException("error creating hash!");
    }

    /**
     * Uses a combination of the stored password hash and account id to salt and create a secret
     * key that will actually be used for encryption. This process is lengthy, so the key should
     * be stored in memory until the app specifically removes it.
     *
     * @param hash      the password hash created using hashPassword().
     * @param accountId the account id that will be used to create the key in conjunction with the
     *                  hash.
     * @param salt      the salt to use (should be gotten from the server after a successful login
     *                  request). This should not be the same salt that was used for hashPassword().
     * @return a secret key we can use to encrypt and decryptData data.
     */
    public SecretKey createKey(String hash, String accountId, String salt) {
        String password = accountId + ":" + hash;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8),
                    ITERATIONS, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            System.out.println("could not create key");
            e.printStackTrace();
        }

        throw new RuntimeException("error creating secret key from hash!");
    }

}
