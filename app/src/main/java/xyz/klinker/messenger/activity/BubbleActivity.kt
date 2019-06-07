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

package xyz.klinker.messenger.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.activity.main.*
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.view.WhitableToolbar
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider
import xyz.klinker.messenger.utils.UpdateUtils
import android.view.inputmethod.InputMethodManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.klinker.messenger.fragment.PrivateConversationListFragment
import xyz.klinker.messenger.fragment.settings.MyAccountFragment
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.util.*


/**
 * Main entry point to the app. This will serve for setting up the drawer view, finding
 * conversations and displaying things on the screen to get the user started.
 */
class BubbleActivity : MessengerActivity()
