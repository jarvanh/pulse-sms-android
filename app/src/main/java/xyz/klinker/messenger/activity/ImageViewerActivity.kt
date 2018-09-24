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
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import xyz.klinker.android.drag_dismiss.util.StatusBarHelper
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ImageViewerAdapter
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.MediaSaver

/**
 * Activity that allows you to scroll between images in a given conversation.
 */
@SuppressLint("InlinedApi")
class ImageViewerActivity : AppCompatActivity() {

    private val viewPager: ViewPager by lazy { findViewById<View>(R.id.view_pager) as ViewPager }
    private val messages = mutableListOf<Message>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AndroidVersionUtil.isAndroidO) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }

        setContentView(R.layout.activity_image_viewer)
        loadMessages()
        initViewPager()
        initToolbar()

        ActivityUtils.setTaskDescription(this, "", Color.BLACK)
    }

    private fun loadMessages() {
        if (intent.extras == null || !intent.extras!!.containsKey(EXTRA_CONVERSATION_ID)) {
            finish()
            return
        }

        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)

        messages.addAll(DataSource.getMediaMessages(this, conversationId))
        if (messages.size == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.no_media, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.close) { finish() }.show()
        }
    }

    private fun initViewPager() {
        if (messages.isEmpty()) {
            return
        }

        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1)

        viewPager.adapter = ImageViewerAdapter(supportFragmentManager, messages)

        if (messageId != -1L) {
            for (i in messages.indices) {
                if (messages[i].id == messageId) {
                    viewPager.setCurrentItem(i, false)
                    break
                }
            }
        } else {
            viewPager.setCurrentItem(viewPager.adapter?.count ?: 1 - 1, false)
        }
    }

    private fun initToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        title = ""
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        (toolbar.layoutParams as AppBarLayout.LayoutParams).topMargin =
                StatusBarHelper.getStatusBarHeight(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when {
            item.itemId == android.R.id.home -> finish()
            item.itemId == R.id.save -> {
                val message = messages[viewPager.currentItem]
                MediaSaver(this as Activity?).saveMedia(message)
            }
            item.itemId == R.id.share -> {
                val message = messages[viewPager.currentItem]
                shareMessage(message)
            }
        }

        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            return
        }

        if (requestCode == PERMISSION_STORAGE_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            MediaSaver(this as Activity?).saveMedia(messages[viewPager.currentItem])
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_STORAGE_REQUEST)
        }
    }

    private fun shareMessage(message: Message) {
        val contentUri = ImageUtils.createContentUri(this, Uri.parse(message.data))

        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.type = message.mimeType
        startActivity(Intent.createChooser(shareIntent,
                resources.getText(R.string.share_content)))
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
        val EXTRA_MESSAGE_ID = "message_id"
        private val PERMISSION_STORAGE_REQUEST = 1
    }
}
