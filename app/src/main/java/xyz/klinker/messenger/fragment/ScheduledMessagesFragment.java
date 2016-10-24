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

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.IllegalFormatConversionException;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ScheduledMessagesAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.service.ScheduledMessageService;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.listener.ScheduledMessageClickListener;

/**
 * Fragment for displaying scheduled messages.
 */
public class ScheduledMessagesFragment extends Fragment implements ScheduledMessageClickListener {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PHONE_NUMBERS = "phone_numbers";

    private RecyclerView list;
    private ProgressBar progress;
    private FloatingActionButton fab;
    private View emptyView;

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
        View view = inflater.inflate(R.layout.fragment_schedule_messages, parent, false);

        list = (RecyclerView) view.findViewById(R.id.list);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        emptyView = view.findViewById(R.id.empty_view);

        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSchedulingMessage();
            }
        });

        Settings settings = Settings.get(getActivity());
        if (settings.useGlobalThemeColor) {
            emptyView.setBackgroundColor(settings.globalColorSet.colorLight);
            fab.setBackgroundTintList(ColorStateList.valueOf(settings.globalColorSet.colorAccent));
            ColorUtils.changeRecyclerOverscrollColors(list, settings.globalColorSet.color);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadMessages();

        if (getArguments() != null && getArguments().getString(ARG_TITLE) != null &&
                getArguments().getString(ARG_PHONE_NUMBERS) != null) {
            ScheduledMessage message = new ScheduledMessage();
            message.to = getArguments().getString(ARG_PHONE_NUMBERS);
            message.title = getArguments().getString(ARG_TITLE);
            displayDateDialog(message);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().startService(new Intent(getActivity(), ScheduledMessageService.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void loadMessages() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final DataSource source = DataSource.getInstance(getActivity());
                source.open();
                final Cursor messages = source.getScheduledMessages();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setMessages(messages);
                        source.close();
                    }
                });
            }
        }).start();
    }

    private void setMessages(Cursor messages) {
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
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.delete_scheduled_message, message.title))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DataSource source = DataSource.getInstance(getActivity());
                        source.open();
                        source.deleteScheduledMessage(message.id);
                        source.close();

                        loadMessages();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startSchedulingMessage() {
        ScheduledMessage message = new ScheduledMessage();
        displayNameDialog(message);
    }

    private void displayNameDialog(final ScheduledMessage message) {
        //noinspection AndroidLintInflateParams
        View layout = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_recipient_edit_text, null, false);
        final RecipientEditTextView editText = (RecipientEditTextView)
                layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.scheduled_to_hint);
        editText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter adapter =
                new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, getActivity());
        adapter.setShowMobileOnly(Settings.get(getActivity()).mobileOnly);
        editText.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (editText.getRecipients().length > 0) {
                            StringBuilder to = new StringBuilder();
                            StringBuilder title = new StringBuilder();

                            for (DrawableRecipientChip chip : editText.getRecipients()) {
                                to.append(PhoneNumberUtils.clearFormatting(chip.getEntry().getDestination()));
                                title.append(chip.getEntry().getDisplayName());
                                to.append(", ");
                                title.append(", ");
                            }

                            message.to = to.toString();
                            message.title = title.toString();

                            message.to = message.to.substring(0, message.to.length() - 2);
                            message.title = message.title.substring(0, message.title.length() - 2);
                        } else if (editText.getText().length() > 0) {
                            message.to = PhoneNumberUtils.clearFormatting(editText
                                    .getText().toString());
                            message.title = message.to;
                        } else {
                            displayNameDialog(message);
                            return;
                        }

                        dismissKeyboard(editText);
                        displayDateDialog(message);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void displayDateDialog(final ScheduledMessage message) {
        Context context = getContextToFixDatePickerCrash();

        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                message.timestamp = new GregorianCalendar(year, month, day)
                        .getTimeInMillis();
                displayTimeDialog(message);
            }
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void displayTimeDialog(final ScheduledMessage message) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                message.timestamp += (1000 * 60 * 60 * hourOfDay);
                message.timestamp += (1000 * 60 * minute);

                if (message.timestamp > System.currentTimeMillis()) {
                    displayMessageDialog(message);
                } else {
                    Toast.makeText(getActivity(), R.string.scheduled_message_in_future,
                            Toast.LENGTH_SHORT).show();
                    displayDateDialog(message);
                }
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(getActivity()))
                .show();
    }

    private void displayMessageDialog(final ScheduledMessage message) {
        //noinspection AndroidLintInflateParams
        View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_text,
                null, false);
        final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
        editText.setHint(R.string.scheduled_message_hint);

        new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (editText.getText().length() > 0) {
                            message.data = editText.getText().toString();
                            message.mimeType = MimeType.TEXT_PLAIN;
                            saveMessage(message);
                        } else {
                            displayMessageDialog(message);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveMessage(final ScheduledMessage message) {
        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        source.insertScheduledMessage(message);
        source.close();

        loadMessages();
    }

    private void dismissKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    // samsung messed up the date picker in some languages on Lollipop 5.0 and 5.1. Ugh.
    // fixes this issue: http://stackoverflow.com/a/34853067
    private ContextWrapper getContextToFixDatePickerCrash() {
        return new ContextWrapper(getActivity()) {

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
