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

package xyz.klinker.messenger.shared.data

/**
 * Holds data for the conversation list adapter on what type of section and how many items are
 * in that section.
 */
class SectionType(var type: Int, var count: Int) {

    override fun equals(other: Any?): Boolean {
        if (other is SectionType) {
            if (other.type == this.type && other.count == this.count) {
                return true
            }
        }

        return false
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + count
        return result
    }

    companion object {
        val CARD_ABOUT_ONLINE = -1
        val PINNED = 0
        val TODAY = 1
        val YESTERDAY = 2
        val LAST_WEEK = 3
        val LAST_MONTH = 4
        val OLDER = 5
    }

}
