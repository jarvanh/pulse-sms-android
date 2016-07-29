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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import xyz.klinker.messenger.R;

/**
 * Fragment for viewing an image using Chris Banes's PhotoView library.
 */
public class ImageViewerFragment extends Fragment {

    private static final String ARG_DATA_URI = "data_uri";
    private static final String ARG_DATA_MIME_TYPE = "mime_type";

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

        PhotoView photo = (PhotoView) view.findViewById(R.id.photo);
        final PhotoViewAttacher attacher = new PhotoViewAttacher(photo);

        Glide.with(getActivity())
                .load(Uri.parse(getArguments().getString(ARG_DATA_URI)))
                .fitCenter()
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model,
                                                   Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        attacher.update();
                        return false;
                    }
                })
                .into(photo);

        return view;
    }

}
