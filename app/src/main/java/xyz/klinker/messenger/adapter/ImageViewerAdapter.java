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

package xyz.klinker.messenger.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.fragment.ImageViewerFragment;

/**
 * Adapter for a view pager that displays images.
 */
public class ImageViewerAdapter extends FragmentPagerAdapter {

    private List<Message> messages;

    public ImageViewerAdapter(FragmentManager fm, List<Message> messages) {
        super(fm);
        this.messages = messages;
    }

    @Override
    public Fragment getItem(int position) {
        Message message = messages.get(position);
        return ImageViewerFragment.newInstance(message.data, message.mimeType);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

}
