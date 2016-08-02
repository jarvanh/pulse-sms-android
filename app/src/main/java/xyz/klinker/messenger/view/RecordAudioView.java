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

package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.listener.AudioRecordedListener;

/**
 * View that allows you to record audio samples and provides a callback to put them back into the
 * ui by their content uri.
 */
@SuppressLint("ViewConstructor")
public class RecordAudioView extends FrameLayout {

    private AudioRecordedListener listener;

    public RecordAudioView(Context context, AudioRecordedListener listener, int color) {
        super(context);

        this.listener = listener;
        init(color);
    }

    private void init(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_record_audio, this, true);
    }

}
