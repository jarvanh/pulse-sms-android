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

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.TvBrowseFragment
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Activity for displaying content on your tv.
 */
class MessengerTvActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = Account
        if (account.accountId == null) {
            startActivity(Intent(this, InitialLoadTvActivity::class.java))
            finish()
            return
        }

        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, TvBrowseFragment())
                .commit()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val permission = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                permission.data = Uri.parse("package:" + packageName)
                startActivity(permission)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }

        }
    }

    public override fun onStart() {
        super.onStart()
        TimeUtils.setupNightTheme(this)
    }
}
