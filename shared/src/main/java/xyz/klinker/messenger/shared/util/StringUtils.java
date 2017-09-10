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

package xyz.klinker.messenger.shared.util;

import java.util.List;
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

    /**
     * Capitalize the first letter of each word.
     */
    public static String titleize(final String input) {
        StringBuilder output = new StringBuilder(input.length());
        boolean lastCharacterWasWhitespace = true;

        for (int i = 0; i < input.length(); i++) {
            char currentCharacter = input.charAt(i);

            if (lastCharacterWasWhitespace && Character.isLowerCase(currentCharacter)) {
                currentCharacter = Character.toTitleCase(currentCharacter);
            }

            output.append(currentCharacter);
            lastCharacterWasWhitespace = Character.isWhitespace(currentCharacter);
        }

        return output.toString();
    }

    /**
     * List of items that should be joined with the separator.
     *
     * @param items
     * @param separator
     *
     * @return the combined string.
     */
    public static <T> String join(final List<T> items, String separator) {
        if (items == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < items.size(); i++) {
            if (i != 0) {
                builder.append(separator);
            }

            builder.append(items.get(i));
        }

        return builder.toString();
    }

    /**
     * List of items that should be joined to create a SQLite OR statement
     *
     * @param items the items to join.
     * @param column the name of the column in the database.
     *
     * @return the combined string
     */
    public static <T> String buildSqlOrStatement(final String column, final List<T> items) {
        if (items == null || items.size() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(column);

        for (int i = 0; i < items.size(); i++) {
            if (i != 0) {
                builder.append(" OR " + column);
            }

            builder.append("=" + items.get(i));
        }

        return builder.toString();
    }

}
