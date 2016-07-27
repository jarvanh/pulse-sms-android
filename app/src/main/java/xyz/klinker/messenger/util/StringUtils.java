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

package xyz.klinker.messenger.util;

import java.util.Random;

/**
 * Utility for working with different strings.
 */
public class StringUtils {

    /**
     * Generates a hexadecimal string of the specified length.
     *
     * @param length the length of the string.
     * @return the hex string.
     */
    public static String generateHexString(int length) {
        Random rand = new Random();
        String id = "";

        for (int i = 0; i < length; i++) {
            int r = rand.nextInt(0x10);
            id += Integer.toHexString(r);
        }

        return id;
    }

}
