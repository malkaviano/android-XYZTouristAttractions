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

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log

import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.common.Utils
import com.example.android.xyztouristattractions.ui.AttractionsActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * A Wear listener service, used to receive inbound messages from
 * other devices.
 */
class ListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer?) {
        Log.d(TAG, "onDataChanged: " + dataEvents!!)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED
                    && event.dataItem != null
                    && Constants.ATTRACTION_PATH == event.dataItem.uri.path) {

                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val attractionsData = dataMapItem.dataMap.getDataMapArrayList(Constants.EXTRA_ATTRACTIONS)
                showNotification(dataMapItem.uri, attractionsData)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent?) {
        Log.v(TAG, "onMessageReceived: " + messageEvent!!)

        if (Constants.CLEAR_NOTIFICATIONS_PATH == messageEvent.path) {
            // Clear the local notification
            UtilityService.clearNotification(this)
        }
    }

    private fun showNotification(attractionsUri: Uri, attractions: ArrayList<DataMap>) {
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (!connectionResult.isSuccess || !googleApiClient.isConnected) {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.errorCode))
            return
        }

        val intent = Intent(this, AttractionsActivity::class.java)
        // Pass through the data Uri as an extra
        intent.putExtra(Constants.EXTRA_ATTRACTIONS_URI, attractionsUri)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val count = attractions.size

        val attraction = attractions[0]

        val bitmap = Utils.loadBitmapFromAsset(
                googleApiClient, attraction.getAsset(Constants.EXTRA_IMAGE))

        val deletePendingIntent = PendingIntent.getService(
                this, 0, UtilityService.getClearRemoteNotificationsIntent(this), 0)

        val notification = NotificationCompat.Builder(this)
                .setContentText(resources.getQuantityString(
                        R.plurals.attractions_found, count, count))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDeleteIntent(deletePendingIntent)
                .addAction(NotificationCompat.Action.Builder(R.drawable.ic_full_explore,
                        getString(R.string.action_explore),
                        pendingIntent).build())
                .extend(NotificationCompat.WearableExtender()
                        .setBackground(bitmap)
                )
                .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(Constants.WEAR_NOTIFICATION_ID, notification)

        googleApiClient.disconnect()
    }

    companion object {
        private val TAG = ListenerService::class.java.simpleName
    }
}
