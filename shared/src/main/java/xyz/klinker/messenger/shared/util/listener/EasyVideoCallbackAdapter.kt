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

package xyz.klinker.messenger.shared.util.listener

import android.net.Uri

import com.afollestad.easyvideoplayer.EasyVideoCallback
import com.afollestad.easyvideoplayer.EasyVideoPlayer

/**
 * Not all of these need to be implemented in the image viewer fragment and they clutter up that
 * class. So put them here and override these empty methods if we need to do something somewhere.
 */
class EasyVideoCallbackAdapter : EasyVideoCallback {

    override fun onStarted(player: EasyVideoPlayer) { }
    override fun onPaused(player: EasyVideoPlayer) { }
    override fun onPreparing(player: EasyVideoPlayer) { }
    override fun onPrepared(player: EasyVideoPlayer) { }
    override fun onBuffering(percent: Int) { }
    override fun onError(player: EasyVideoPlayer, e: Exception) { }
    override fun onCompletion(player: EasyVideoPlayer) { }
    override fun onRetry(player: EasyVideoPlayer, source: Uri) { }
    override fun onSubmit(player: EasyVideoPlayer, source: Uri) { }

}
