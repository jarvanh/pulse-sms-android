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

package xyz.klinker.messenger.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.fragment.ImageViewerFragment

/**
 * Adapter for a view pager that displays images.
 */
class ImageViewerAdapter(fm: FragmentManager, private val messages: List<Message>) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val message = messages[position]
        return ImageViewerFragment.newInstance(message.data!!, message.mimeType!!)
    }

    override fun getCount() = messages.size
}
