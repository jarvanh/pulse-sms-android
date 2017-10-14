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

package xyz.klinker.messenger.shared.view;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.util.NotificationWindowManager;

/**
 * The main icon to be displayed when a notification is received.
 */
public class NotificationView {

    private NotificationWindowManager manager;
    private View view;
    private ImageView icon;
    private TextView text;
    private String description;
    private String title;

    /**
     * Create a new instance of a notification icon.
     * @param manager the window manager responsible for displaying the notification.
     * @return the NotificationView.
     */
    public static NotificationView newInstance(NotificationWindowManager manager) {
        return new NotificationView(manager);
    }

    /**
     * Creates a new instance of NotificationView.
     * @param manager the notification window manager to attach icon to.
     */
    private NotificationView(NotificationWindowManager manager) {
        init(manager);
    }

    /**
     * Initializes the icon with relevant information.
     */
    @SuppressLint("InflateParams")
    private void init(NotificationWindowManager manager) {
        this.manager = manager;

        LayoutInflater inflater = LayoutInflater.from(manager.getService());
        this.view = inflater.inflate(R.layout.notification_view, null, false);
        this.icon = (ImageView) view.findViewById(R.id.icon);
        this.text = (TextView) view.findViewById(R.id.text);
    }

    /**
     * Sets an image for the notification.
     * @param drawable the image to show.
     * @return the notification icon.
     */
    public NotificationView setImage(Drawable drawable) {
        if (drawable != null) {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(drawable);
        }
        return this;
    }

    /**
     * Sets the description for the notification.
     * @param text the description to show on the notification.
     * @return the notification.
     */
    public NotificationView setDescription(String text) {
        this.description = text;
        return this;
    }

    /**
     * Sets the title for the notification.
     * @param title the title to show on the notification.
     * @return the notification.
     */
    public NotificationView setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Shows the notification on the screen.
     */
    public void show() {
        text.setText(createText());
        manager.addOverlayView(view);
    }

    /**
     * Creates a text string with bolded title and regular description when each is available.
     * @return the formatted text.
     */
    private Spanned createText() {
        return Html.fromHtml(createTextString());
    }

    /**
     * Creates a text string with bolded title and description when each is available. This is a
     * string with can be html formatted and passed into the textview.
     * @return the formatted string.
     */
    private String createTextString() {
        String text = "";

        if (title != null) {
            text = "<b>" + title + ":</b> ";
        }

        if (description != null) {
            text += description;
        }

        return text;
    }

    /**
     * Hides the notification from the screen.
     */
    public void hide() {
        manager.removeOverlayView(view);
    }

    /**
     * Gets the notification window manager.
     * @return the manager.
     */
    public NotificationWindowManager getManager() {
        return manager;
    }

    /**
     * Sets the notification window manager.
     * @param manager the manager.
     */
    public void setManager(NotificationWindowManager manager) {
        this.manager = manager;
    }

    /**
     * Gets the icon being displayed.
     * @return the icon.
     */
    public View getView() {
        return view;
    }

}
