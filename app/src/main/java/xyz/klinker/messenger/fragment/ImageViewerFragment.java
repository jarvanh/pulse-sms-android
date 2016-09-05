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

package xyz.klinker.messenger.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.easyvideoplayer.EasyVideoPlayer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import uk.co.senab.photoview.PhotoView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.util.listener.EasyVideoCallbackAdapter;

/**
 * Fragment for viewing an image using Chris Banes's PhotoView library.
 */
public class ImageViewerFragment extends Fragment {

    private static final String ARG_DATA_URI = "data_uri";
    private static final String ARG_DATA_MIME_TYPE = "mime_type";

    private EasyVideoPlayer player;

    public static ImageViewerFragment newInstance(String uri, String mimeType) {
        ImageViewerFragment fragment = new ImageViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATA_URI, uri);
        args.putString(ARG_DATA_MIME_TYPE, mimeType);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_viewer, parent, false);

        player = (EasyVideoPlayer) view.findViewById(R.id.player);
        PhotoView photo = (PhotoView) view.findViewById(R.id.photo);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(photo);

        String data = getArguments().getString(ARG_DATA_URI);
        String mimeType = getArguments().getString(ARG_DATA_MIME_TYPE);

        if (MimeType.isStaticImage(mimeType)) {
            Glide.with(getActivity())
                    .load(Uri.parse(data))
                    .fitCenter()
                    .into(target);
        } else if (MimeType.isVideo(mimeType) || MimeType.isAudio(mimeType)) {
            player.setVisibility(View.VISIBLE);
            photo.setVisibility(View.GONE);
            player.setCallback(new EasyVideoCallbackAdapter());
            player.setLeftAction(EasyVideoPlayer.LEFT_ACTION_NONE);
            player.setRightAction(EasyVideoPlayer.RIGHT_ACTION_NONE);
            player.setSource(Uri.parse(data));

            if (Settings.get(getActivity()).useGlobalThemeColor) {
                player.setThemeColor(Settings.get(getActivity()).globalColorSet.color);
            }

            if (MimeType.isAudio(mimeType)) {
                view.findViewById(R.id.audio).setVisibility(View.VISIBLE);
                player.setHideControlsOnPlay(false);
            }
        } else {
            Glide.with(getActivity())
                    .load(Uri.parse(data))
                    .into(target);
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        player.pause();
    }

}
