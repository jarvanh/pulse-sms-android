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

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;

import static junit.framework.Assert.assertEquals;

public class ColorUtilsTest extends MessengerRobolectricSuite {

    @Test
    public void noRandomOutOfBounds() {
        for (int i = 0; i < 100; i++) {
            ColorUtils.INSTANCE.getRandomMaterialColor(RuntimeEnvironment.application);
        }
    }

    @Test
    public void convertToHex() {
        assertEquals("#F44336", ColorUtils.INSTANCE.convertToHex(RuntimeEnvironment.application
                .getResources().getColor(R.color.materialRed)));
    }

}