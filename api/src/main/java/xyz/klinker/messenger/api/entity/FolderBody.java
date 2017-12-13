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

package xyz.klinker.messenger.api.entity;

public class FolderBody {

    public long deviceId;
    public String name;
    public int color;
    public int colorDark;
    public int colorLight;
    public int colorAccent;

    public FolderBody(long deviceId, String name, int color, int colorDark, int colorLight, int colorAccent) {
        this.deviceId = deviceId;
        this.name = name;
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
    }

    @Override
    public String toString() {
        return deviceId + ", " + name + ", " + color + ", " + colorDark + ", " + colorLight + ", " + colorAccent;
    }

}
