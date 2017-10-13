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

package xyz.klinker.messenger.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.IllegalFormatConversionException;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ScheduledMessagesAdapter;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.fragment.bottom_sheet.EditScheduledMessageFragment;
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.listener.ScheduledMessageClickListener;

/**
 * Fragment for displaying scheduled messages.
 */
public class ScheduledMessagesFragment extends Fragment implements ScheduledMessageClickListener {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PHONE_NUMBERS = "phone_numbers";
    
    private FragmentActivity activity;

    private RecyclerView list;
    private ProgressBar progress;
    private FloatingActionButton fab;
    private View emptyView;

    private BroadcastReceiver scheduledMessageSent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadMessages();
        }
    };

    public static ScheduledMessagesFragment newInstance() {
        return new ScheduledMessagesFragment();
    }

    public static ScheduledMessagesFragment newInstance(String title, String phoneNumbers) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_PHONE_NUMBERS, phoneNumbers);

        ScheduledMessagesFragment fragment = new ScheduledMessagesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.activity = getActivity();

        View view = inflater.inflate(R.layout.fragment_schedule_messages, parent, false);

        list = (RecyclerView) view.findViewById(R.id.list);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        emptyView = view.findViewById(R.id.empty_view);

        list.setLayoutManager(new LinearLayoutManager(activity));
        fab.setOnClickListener(view1 -> startSchedulingMessage());

        emptyView.setBackgroundColor(Settings.INSTANCE.getMainColorSet().getColorLight());
        fab.setBackgroundTintList(ColorStateList.valueOf(Settings.INSTANCE.getMainColorSet().getColorAccent()));
        ColorUtils.INSTANCE.changeRecyclerOverscrollColors(list, Settings.INSTANCE.getMainColorSet().getColor());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadMessages();

        if (getArguments() != null && getArguments().getString(ARG_TITLE) != null &&
                getArguments().getString(ARG_PHONE_NUMBERS) != null) {
            ScheduledMessage message = new ScheduledMessage();
            message.setTo(getArguments().getString(ARG_PHONE_NUMBERS));
            message.setTitle(getArguments().getString(ARG_TITLE));
            displayDateDialog(message);
        }

        fab.setOnClickListener(view1 -> startSchedulingMessage());
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.registerReceiver(scheduledMessageSent,
                new IntentFilter(ScheduledMessageJob.BROADCAST_SCHEDULED_SENT));
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            activity.unregisterReceiver(scheduledMessageSent);
        } catch (Exception e) {

        }

        ScheduledMessageJob.scheduleNextRun(activity);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void loadMessages() {
        final Handler handler = new Handler();
        new Thread(() -> {
            if (activity != null) {
                final List<ScheduledMessage> messages =
                        DataSource.INSTANCE.getScheduledMessagesAsList(activity);
                handler.post(() -> setMessages(messages));
            }
        }).start();
    }

    private void setMessages(List<ScheduledMessage> messages) {
        progress.setVisibility(View.GONE);
        list.setAdapter(new ScheduledMessagesAdapter(messages, this));

        if (list.getAdapter().getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(final ScheduledMessage message) {
        EditScheduledMessageFragment fragment = new EditScheduledMessageFragment();
        fragment.setMessage(message);
        fragment.setFragment(this);
        fragment.show(activity.getSupportFragmentManager(), "");
    }

    private void startSchedulingMessage() {
        ScheduledMessage message = new ScheduledMessage();
        displayNameDialog(message);
    }

    private void displayNameDialog(final ScheduledMessage message) {
        //noinspection AndroidLintInflateParams
        View layout = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_recipient_edit_text, null, false);
        final RecipientEditTextView editText = (RecipientEditTextView)
                layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.scheduled_to_hint);
        editText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter adapter =
                new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, activity);
        adapter.setShowMobileOnly(Settings.INSTANCE.getMobileOnly());
        editText.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    if (editText.getRecipients().length > 0) {
                        StringBuilder to = new StringBuilder();
                        StringBuilder title = new StringBuilder();

                        for (DrawableRecipientChip chip : editText.getRecipients()) {
                            to.append(PhoneNumberUtils.clearFormatting(chip.getEntry().getDestination()));
                            title.append(chip.getEntry().getDisplayName());
                            to.append(", ");
                            title.append(", ");
                        }

                        message.setTo(to.toString());
                        message.setTitle(title.toString());

                        message.setTo(message.getTo().substring(0, message.getTo().length() - 2));
                        message.setTitle(message.getTitle().substring(0, message.getTitle().length() - 2));
                    } else if (editText.getText().length() > 0) {
                        message.setTo(PhoneNumberUtils.clearFormatting(editText
                                .getText().toString()));
                        message.setTitle(message.getTo());
                    } else {
                        displayNameDialog(message);
                        return;
                    }

                    dismissKeyboard(editText);
                    displayDateDialog(message);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void displayDateDialog(final ScheduledMessage message) {
        Context context = getContextToFixDatePickerCrash();

        if (context == null) {
            context = activity;
        }
        
        if (context == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, (datePicker, year, month, day) -> {
            message.setTimestamp(new GregorianCalendar(year, month, day)
                    .getTimeInMillis());
            displayTimeDialog(message);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void displayTimeDialog(final ScheduledMessage message) {
        if (activity == null) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(activity, (timePicker, hourOfDay, minute) -> {
            message.setTimestamp(message.getTimestamp() + (1000 * 60 * 60 * hourOfDay));
            message.setTimestamp(message.getTimestamp() + (1000 * 60 * minute));

            if (message.getTimestamp() > System.currentTimeMillis()) {
                displayMessageDialog(message);
            } else {
                Toast.makeText(activity, R.string.scheduled_message_in_future,
                        Toast.LENGTH_SHORT).show();
                displayDateDialog(message);
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(activity))
                .show();
    }

    private void displayMessageDialog(final ScheduledMessage message) {
        //noinspection AndroidLintInflateParams
        View layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text,
                null, false);
        final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.scheduled_message_hint);

        new AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.add, (dialogInterface, i) -> {
                    if (editText.getText().length() > 0) {
                        message.setData(editText.getText().toString());
                        message.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
                        saveMessage(message);
                    } else {
                        displayMessageDialog(message);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveMessage(final ScheduledMessage message) {
        DataSource.INSTANCE.insertScheduledMessage(activity, message);
        loadMessages();
    }

    private void dismissKeyboard(EditText editText) {
        if (editText == null) {
            return;
        }

        try {
            InputMethodManager imm = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        } catch (Exception e) {

        }
    }

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private ContextWrapper getContextToFixDatePickerCrash() {
        return new ContextWrapper(activity) {

            private Resources wrappedResources;

            @Override
            public Resources getResources() {
                Resources r = super.getResources();
                if(wrappedResources == null) {
                    wrappedResources = new Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration()) {
                        @NonNull
                        @Override
                        public String getString(int id, Object... formatArgs) throws NotFoundException {
                            try {
                                return super.getString(id, formatArgs);
                            } catch (IllegalFormatConversionException ifce) {
                                Log.e("DatePickerDialogFix", "IllegalFormatConversionException Fixed!", ifce);
                                String template = super.getString(id);
                                template = template.replaceAll("%" + ifce.getConversion(), "%s");
                                return String.format(getConfiguration().locale, template, formatArgs);
                            }
                        }
                    };
                }
                return wrappedResources;
            }
        };
    }

}
