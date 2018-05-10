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

package xyz.klinker.messenger.utils.swipe_to_dismiss

import android.support.v7.widget.helper.ItemTouchHelper

import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.utils.swipe_to_dismiss.setup.SwipeSetupBase
import xyz.klinker.messenger.utils.swipe_to_dismiss.setup.SwipeSetupCustom

/**
 * A touch helper that uses the SwipeSetupBase as an impementation.
 */
class SwipeTouchHelper : ItemTouchHelper {

    constructor(adapter: ConversationListAdapter) : super(SwipeSetupCustom(adapter))
    constructor(callback: SwipeSetupBase) : super(callback)

}
