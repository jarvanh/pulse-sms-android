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

package xyz.klinker.messenger.fragment

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.afollestad.easyvideoplayer.EasyVideoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import uk.co.senab.photoview.PhotoView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.listener.EasyVideoCallbackAdapter

/**
 * Fragment for viewing an image using Chris Banes's PhotoView library.
 */
class ImageViewerFragment : Fragment() {

    private var player: EasyVideoPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_image_viewer, parent, false)

        val fragmentActivity = activity ?: return null

        player = view.findViewById<View>(R.id.player) as EasyVideoPlayer
        val photo = view.findViewById<View>(R.id.photo) as PhotoView

        val data = arguments?.getString(ARG_DATA_URI)
        val mimeType = arguments?.getString(ARG_DATA_MIME_TYPE)

        if (MimeType.isStaticImage(mimeType)) {
            Glide.with(fragmentActivity)
                    .load(Uri.parse(data))
                    .apply(RequestOptions().fitCenter())
                    .into(photo)
        } else if (MimeType.isVideo(mimeType!!) || MimeType.isAudio(mimeType)) {
            photo.visibility = View.GONE

            player?.visibility = View.VISIBLE
            player?.setCallback(EasyVideoCallbackAdapter())
            player?.setLeftAction(EasyVideoPlayer.LEFT_ACTION_NONE)
            player?.setRightAction(EasyVideoPlayer.RIGHT_ACTION_NONE)
            player?.setSource(Uri.parse(data))
            player?.setThemeColor(Settings.mainColorSet.color)

            if (MimeType.isAudio(mimeType)) {
                view.findViewById<View>(R.id.audio).visibility = View.VISIBLE
                player?.setHideControlsOnPlay(false)
            }
        } else {
            Glide.with(fragmentActivity)
                    .load(Uri.parse(data))
                    .into(photo)
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    companion object {
        private val ARG_DATA_URI = "data_uri"
        private val ARG_DATA_MIME_TYPE = "mime_type"

        fun newInstance(uri: String, mimeType: String): ImageViewerFragment {
            val fragment = ImageViewerFragment()
            val args = Bundle()
            args.putString(ARG_DATA_URI, uri)
            args.putString(ARG_DATA_MIME_TYPE, mimeType)

            fragment.arguments = args
            return fragment
        }
    }

}
