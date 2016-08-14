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

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {

    public byte[] deflate(byte[] data) {
        try {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream(data.length);
            try {
                GZIPOutputStream zipStream =
                        new GZIPOutputStream(byteStream);
                try {
                    zipStream.write(data);
                } finally {
                    zipStream.close();
                }
            } finally {
                byteStream.close();
            }

            return byteStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
