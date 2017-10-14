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

package xyz.klinker.messenger.shared.service

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.ContactImageCreator
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.closeSilent
import java.util.*

@TargetApi(Build.VERSION_CODES.M)
class MessengerChooserTargetService : ChooserTargetService() {

    override fun onGetChooserTargets(componentName: ComponentName,
                                     intentFilter: IntentFilter): List<ChooserTarget> {
        val targets = ArrayList<ChooserTarget>()

        val source = DataSource
        var cursor = source.getPinnedConversations(this)

        if (cursor.count == 0) {
            cursor.closeSilent()
            cursor = source.getUnarchivedConversations(this)
        }

        if (cursor.moveToFirst()) {
            do {
                targets.add(createTarget(cursor, componentName))
            } while (cursor.moveToNext() && targets.size < 5)
        }

        cursor.closeSilent()
        return targets
    }

    private fun createTarget(cursor: Cursor, componentName: ComponentName): ChooserTarget {
        val conversation = Conversation()
        conversation.fillFromCursor(cursor)

        val image = ImageUtils.getBitmap(this, conversation.imageUri)

        val targetIcon = if (image == null) {
            val color = ContactImageCreator.getLetterPicture(this, conversation)
            Icon.createWithBitmap(color)
        } else {
            Icon.createWithBitmap(ImageUtils.clipToCircle(image))
        }

        val targetExtras = Bundle()
        targetExtras.putLong(EXTRA_CONVO_ID, cursor.getLong(
                cursor.getColumnIndex(Conversation.COLUMN_ID)))

        return ChooserTarget(conversation.title, targetIcon, 1.0f,
                componentName, targetExtras)

    }

    companion object {
        val EXTRA_CONVO_ID = "chooser_target_convo_id"
    }
}
