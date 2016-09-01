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

package xyz.klinker.messenger.data;

/**
 * Holds data for the conversation list adapter on what type of section and how many items are
 * in that section.
 */
public class SectionType {

    public static final int PINNED = 0;
    public static final int TODAY = 1;
    public static final int YESTERDAY = 2;
    public static final int LAST_WEEK = 3;
    public static final int LAST_MONTH = 4;
    public static final int OLDER = 5;

    public int type;
    public int count;

    public SectionType(int type, int count) {
        this.type = type;
        this.count = count;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SectionType) {
            if (((SectionType) obj).type == this.type && ((SectionType) obj).count == this.count) {
                return true;
            }
        }

        return false;
    }

}
