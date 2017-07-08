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

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log

import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.common.Utils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.CapabilityApi
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

/**
 * A utility IntentService, used for a variety of asynchronous background
 * operations that do not necessarily need to be tied to a UI.
 */
class UtilityService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action
        if (ACTION_CLEAR_NOTIFICATION == action) {
            clearNotificationInternal()
        } else if (ACTION_CLEAR_REMOTE_NOTIFICATIONS == action) {
            clearRemoteNotificationsInternal()
        } else if (ACTION_START_DEVICE_ACTIVITY == action) {
            startDeviceActivityInternal(intent.getStringExtra(EXTRA_START_PATH),
                    intent.getStringExtra(EXTRA_START_ACTIVITY_INFO))
        }
    }

    /**
     * Clear the local notifications
     */
    private fun clearNotificationInternal() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.WEAR_NOTIFICATION_ID)
    }

    /**
     * Trigger a message to ask other devices to clear their notifications
     */
    private fun clearRemoteNotificationsInternal() {
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (connectionResult.isSuccess && googleApiClient.isConnected) {
            val itr = Utils.getNodes(googleApiClient).iterator()
            while (itr.hasNext()) {
                // Loop through all connected nodes
                Wearable.MessageApi.sendMessage(
                        googleApiClient, itr.next(), Constants.CLEAR_NOTIFICATIONS_PATH, null)
            }
        }

        googleApiClient.disconnect()
    }

    /**
     * Sends the actual message to ask other devices that are capable of showing "details" to start
     * the appropriate activity

     * @param path the path to pass to the wearable message API
     * *
     * @param extraInfo extra info that varies based on the path being sent
     */
    private fun startDeviceActivityInternal(path: String, extraInfo: String) {
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (connectionResult.isSuccess && googleApiClient.isConnected) {
            val result = Wearable.CapabilityApi.getCapability(
                    googleApiClient,
                    applicationContext.getString(R.string.show_detail_capability_name),
                    CapabilityApi.FILTER_REACHABLE)
                    .await(GET_CAPABILITY_TIMEOUT_S, TimeUnit.SECONDS)
            if (result.status.isSuccess) {
                val nodes = result.capability.nodes
                for (node in nodes) {
                    Wearable.MessageApi.sendMessage(
                            googleApiClient, node.id, path, extraInfo.toByteArray())
                }
            } else {
                Log.e(TAG, "startDeviceActivityInternal() Failed to get capabilities, status: " + result.status.statusMessage!!)
            }

            googleApiClient.disconnect()
        }
    }

    companion object {

        private val TAG = UtilityService::class.java.simpleName

        private val ACTION_CLEAR_NOTIFICATION = "clear_notification"
        private val ACTION_CLEAR_REMOTE_NOTIFICATIONS = "clear_remote_notifications"
        private val ACTION_START_DEVICE_ACTIVITY = "start_device_activity"
        private val EXTRA_START_PATH = "start_path"
        private val EXTRA_START_ACTIVITY_INFO = "start_activity_info"
        private val GET_CAPABILITY_TIMEOUT_S: Long = 10

        fun clearNotification(context: Context) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_CLEAR_NOTIFICATION
            context.startService(intent)
        }

        fun clearRemoteNotifications(context: Context) {
            context.startService(getClearRemoteNotificationsIntent(context))
        }

        fun getClearRemoteNotificationsIntent(context: Context): Intent {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_CLEAR_REMOTE_NOTIFICATIONS
            return intent
        }

        /**
         * Trigger a message that asks the master device to start an activity.

         * @param context the context
         * *
         * @param path the path that will be sent via the wearable message API
         * *
         * @param name the tourist attraction name
         * *
         * @param city the tourist attraction city
         */
        fun startDeviceActivity(context: Context, path: String, name: String, city: String) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_START_DEVICE_ACTIVITY
            val extraInfo: String
            if (Constants.START_ATTRACTION_PATH == path) {
                extraInfo = name
            } else {
                extraInfo = name + ", " + city
            }
            intent.putExtra(EXTRA_START_ACTIVITY_INFO, extraInfo)
            intent.putExtra(EXTRA_START_PATH, path)
            context.startService(intent)
        }
    }

}
