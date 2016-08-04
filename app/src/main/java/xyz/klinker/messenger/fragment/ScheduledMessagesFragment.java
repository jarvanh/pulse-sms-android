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

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ScheduledMessagesAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.util.listener.ScheduledMessageClickListener;

/**
 * Fragment for displaying scheduled messages.
 */
public class ScheduledMessagesFragment extends Fragment implements ScheduledMessageClickListener {

    private RecyclerView list;
    private ProgressBar progress;
    private FloatingActionButton fab;

    private DataSource source;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_messages, parent, false);

        list = (RecyclerView) view.findViewById(R.id.list);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        list.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        source = DataSource.getInstance(getActivity());
        source.open();

        loadMessages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        source.close();
    }

    private void loadMessages() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor messages = source.getScheduledMessages();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setMessages(messages);
                    }
                });
            }
        }).start();
    }

    private void setMessages(Cursor messages) {
        progress.setVisibility(View.GONE);
        list.setAdapter(new ScheduledMessagesAdapter(messages, this));
    }

    @Override
    public void onClick(final ScheduledMessage message) {
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.delete_scheduled_message, message.title))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        source.deleteScheduledMessage(message.id);
                        loadMessages();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
