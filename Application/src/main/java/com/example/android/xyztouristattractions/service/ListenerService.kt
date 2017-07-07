/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.xyztouristattractions.service

import android.content.Intent
import android.net.Uri
import android.util.Log

import com.example.android.xyztouristattractions.ui.DetailActivity
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.example.android.xyztouristattractions.common.Constants

/**
 * A Wear listener service, used to receive inbound messages from
 * the Wear device.
 */
class ListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        Log.v(TAG, "onMessageReceived: " + messageEvent!!)

        if (Constants.CLEAR_NOTIFICATIONS_PATH == messageEvent.path) {
            // Request for this device to clear its notifications
            UtilityService.clearNotification(this)
        } else if (Constants.START_ATTRACTION_PATH == messageEvent.path) {
            // Request for this device open the attraction detail screen
            // to a specific tourist attraction
            val attractionName = String(messageEvent.data)
            val intent = DetailActivity.getLaunchIntent(this, attractionName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else if (Constants.START_NAVIGATION_PATH == messageEvent.path) {
            // Request for this device to start Maps walking navigation to
            // specific tourist attraction
            val attractionQuery = String(messageEvent.data)
            val uri = Uri.parse(Constants.MAPS_NAVIGATION_INTENT_URI + Uri.encode(attractionQuery))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    companion object {
        private val TAG = ListenerService::class.java.simpleName
    }
}
