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

package xyz.klinker.messenger.shared.util

import java.util.Random

/**
 * Utility for working with different strings.
 */
object StringUtils {

    /**
     * Generates a hexadecimal string of the specified length.
     *
     * @param length the length of the string.
     * @return the hex string.
     */
    fun generateHexString(length: Int): String {
        val rand = Random()
        var id = ""

        for (i in 0 until length) {
            val r = rand.nextInt(0x10)
            id += Integer.toHexString(r)
        }

        return id
    }

    /**
     * Capitalize the first letter of each word.
     */
    fun titleize(input: String): String {
        val input = input.toLowerCase()
        val output = StringBuilder(input.length)
        var lastCharacterWasWhitespace = true

        for (i in 0 until input.length) {
            var currentCharacter = input[i]

            if (lastCharacterWasWhitespace && Character.isLowerCase(currentCharacter)) {
                currentCharacter = Character.toTitleCase(currentCharacter)
            }

            output.append(currentCharacter)
            lastCharacterWasWhitespace = Character.isWhitespace(currentCharacter)
        }

        return output.toString()
    }

    /**
     * List of items that should be joined with the separator.
     *
     * @param items
     * @param separator
     *
     * @return the combined string.
     */
    fun <T> join(items: List<T>?, separator: String): String {
        if (items == null) {
            return ""
        }

        val builder = StringBuilder()

        for (i in items.indices) {
            if (i != 0) {
                builder.append(separator)
            }

            builder.append(items[i])
        }

        return builder.toString()
    }

    /**
     * List of items that should be joined to create a SQLite OR statement
     *
     * @param items the items to join.
     * @param column the name of the column in the database.
     *
     * @return the combined string
     */
    fun <T> buildSqlOrStatement(column: String, items: List<T>?): String {
        if (items == null || items.isEmpty()) {
            return ""
        }

        val builder = StringBuilder(column)

        for (i in items.indices) {
            if (i != 0) {
                builder.append(" OR " + column)
            }

            builder.append("=" + items[i])
        }

        return builder.toString()
    }

}
