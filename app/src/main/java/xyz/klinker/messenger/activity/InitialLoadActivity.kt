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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.klinker.android.send_message.Utils
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ActivateActivity
import xyz.klinker.messenger.api.implementation.LoginActivity
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ApiDownloadService
import xyz.klinker.messenger.shared.service.ApiUploadService
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.ProgressUpdateListener

/**
 * Activity for onboarding and initial database load.
 */
open class InitialLoadActivity : AppCompatActivity(), ProgressUpdateListener {

    private val progress: ProgressBar by lazy { findViewById<View>(R.id.loading_progress) as ProgressBar }
    private var handler: Handler? = null
    private var startUploadAfterSync = false
    private var downloadReceiver: BroadcastReceiver? = null

    private val name: String
        get() {
            try {
                val cursor = contentResolver
                        .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null)

                if (cursor != null && cursor.moveToFirst()) {
                    cursor.moveToFirst()
                    val name = cursor.getString(cursor.getColumnIndex("display_name"))
                    CursorUtil.closeSilent(cursor)
                    return name
                } else {
                    CursorUtil.closeSilent(cursor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ""
        }

    private val phoneNumber: String
        get() = try {
            PhoneNumberUtils.clearFormatting(Utils.getMyPhoneNumber(this))
        } catch (e: Exception) {
            ""
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_load)

        if (intent.getBooleanExtra(UPLOAD_AFTER_SYNC, false)) {
            startUploadAfterSync = true
        }

        handler = Handler()
        requestPermissions()
    }

    private fun requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(this)) {
            PermissionsUtils.startMainPermissionRequest(this)
        } else {
            startLogin()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        try {
            if (PermissionsUtils.processPermissionRequest(this, requestCode, permissions, grantResults)) {
                startLogin()
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    public override fun onActivityResult(requestCode: Int, responseCode: Int, data: Intent?) {
        if (requestCode == SETUP_REQUEST) {
            Settings.forceUpdate(this)

            when (responseCode) {
                Activity.RESULT_CANCELED -> {
                    if (!startUploadAfterSync) {
                        val account = Account
                        account.setDeviceId(this, null)
                        account.setPrimary(this, true)
                    }

                    startDatabaseSync()
                }
                LoginActivity.RESULT_START_DEVICE_SYNC -> {
                    startDatabaseSync()
                    startUploadAfterSync = true
                }
                LoginActivity.RESULT_START_NETWORK_SYNC -> {
                    ApiDownloadService.start(this)
                    downloadReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            close()
                        }
                    }

                    registerReceiver(downloadReceiver, IntentFilter(ApiDownloadService.ACTION_DOWNLOAD_FINISHED))
                }
                ActivateActivity.RESULT_FAILED -> finish()
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver)
            downloadReceiver = null
        }
    }

    private fun startLogin() {
        // we want to pass the extras from the last intent to this one, since they will tell us if
        // we should automatically skip the login and just go into the data load.
        val login = Intent(this, LoginActivity::class.java)
        login.putExtras(intent)

        startActivityForResult(login, SETUP_REQUEST)
    }

    private fun startDatabaseSync() {
        Thread {
            val context = applicationContext
            val startTime = System.currentTimeMillis()

            val myName = name
            val myPhoneNumber = PhoneNumberUtils.format(phoneNumber)

            val account = Account
            account.setName(this, myName)
            account.setPhoneNumber(this, myPhoneNumber)

            val source = DataSource

            val conversations = SmsMmsUtils.queryConversations(context)
            try {
                source.insertConversations(conversations, context, this@InitialLoadActivity)
            } catch (e: Exception) {
                source.ensureActionable(this)
                source.insertConversations(conversations, context, this@InitialLoadActivity)
            }

            handler!!.post { progress.isIndeterminate = true }

            val contacts = ContactUtils.queryContacts(context, source)
            source.insertContacts(this, contacts, null)

            val importTime = System.currentTimeMillis() - startTime
            AnalyticsHelper.importFinished(this, importTime)
            Log.v("initial_load", "load took $importTime ms")

            handler!!.postDelayed({ this.close() }, 5000)
        }.start()
    }

    private fun close() {
        Settings.setValue(this, getString(R.string.pref_first_start), false)

        if (TvUtils.hasTouchscreen(this)) {
            startActivity(Intent(this, MessengerActivity::class.java))
        } else {
            startActivity(Intent(this, MessengerTvActivity::class.java))
        }

        if (startUploadAfterSync) {
            ApiUploadService.start(this)
        }

        finish()
    }

    override fun onProgressUpdate(current: Int, max: Int) {
        handler!!.post {
            progress.isIndeterminate = false
            progress.max = max

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progress.setProgress(current, true)
            } else {
                progress.progress = current
            }
        }
    }

    override fun onBackPressed() {
        // don't let them back out of this
    }

    companion object {
        val UPLOAD_AFTER_SYNC = "upload_after_sync"
        private val SETUP_REQUEST = 54321
    }
}
