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
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.location.Location
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Attraction
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.common.Utils
import com.example.android.xyztouristattractions.provider.TouristAttractions
import com.example.android.xyztouristattractions.ui.DetailActivity
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import com.example.android.xyztouristattractions.provider.TouristAttractions.ATTRACTIONS
import com.google.android.gms.location.LocationServices.FusedLocationApi
import com.google.android.gms.location.LocationServices.GeofencingApi

/**
 * A utility IntentService, used for a variety of asynchronous background
 * operations that do not necessarily need to be tied to a UI.
 */
class UtilityService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action
        if (ACTION_ADD_GEOFENCES == action) {
            addGeofencesInternal()
        } else if (ACTION_GEOFENCE_TRIGGERED == action) {
            geofenceTriggered(intent)
        } else if (ACTION_REQUEST_LOCATION == action) {
            requestLocationInternal()
        } else if (ACTION_LOCATION_UPDATED == action) {
            locationUpdated(intent)
        } else if (ACTION_CLEAR_NOTIFICATION == action) {
            clearNotificationInternal()
        } else if (ACTION_CLEAR_REMOTE_NOTIFICATIONS == action) {
            clearRemoteNotifications()
        } else if (ACTION_FAKE_UPDATE == action) {
            val currentLocation = Utils.getLocation(this)

            // If location unknown use test city, otherwise use closest city
            val city = if (currentLocation == null)
                TouristAttractions.TEST_CITY
            else
                TouristAttractions.getClosestCity(currentLocation)

            showNotification(city,
                    intent.getBooleanExtra(EXTRA_TEST_MICROAPP, Constants.USE_MICRO_APP))
        }
    }

    /**
     * Add geofences using Play Services
     */
    private fun addGeofencesInternal() {
        Log.v(TAG, ACTION_ADD_GEOFENCES)

        if (!Utils.checkFineLocationPermission(this)) {
            return
        }

        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build()

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (connectionResult.isSuccess && googleApiClient.isConnected) {
            val pendingIntent = PendingIntent.getBroadcast(
                    this, 0, Intent(this, UtilityReceiver::class.java), 0)
            GeofencingApi.addGeofences(googleApiClient,
                    TouristAttractions.geofenceList, pendingIntent)
            googleApiClient.disconnect()
        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.errorCode))
        }
    }

    /**
     * Called when a geofence is triggered
     */
    private fun geofenceTriggered(intent: Intent) {
        Log.v(TAG, ACTION_GEOFENCE_TRIGGERED)

        // Check if geofences are enabled
        val geofenceEnabled = Utils.getGeofenceEnabled(this)

        // Extract the geofences from the intent
        val event = GeofencingEvent.fromIntent(intent)
        val geofences = event.triggeringGeofences

        if (geofenceEnabled && geofences != null && geofences.size > 0) {
            if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Trigger the notification based on the first geofence
                showNotification(geofences[0].requestId, Constants.USE_MICRO_APP)
            } else if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // Clear notifications
                clearNotificationInternal()
                clearRemoteNotifications()
            }
        }

        WakefulBroadcastReceiver.completeWakefulIntent(intent)
    }

    /**
     * Called when a location update is requested
     */
    private fun requestLocationInternal() {
        Log.v(TAG, ACTION_REQUEST_LOCATION)

        if (!Utils.checkFineLocationPermission(this)) {
            return
        }

        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build()

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (connectionResult.isSuccess && googleApiClient.isConnected) {

            val locationUpdatedIntent = Intent(this, UtilityService::class.java)
            locationUpdatedIntent.action = ACTION_LOCATION_UPDATED

            // Send last known location out first if available
            val location = FusedLocationApi.getLastLocation(googleApiClient)
            if (location != null) {
                val lastLocationIntent = Intent(locationUpdatedIntent)
                lastLocationIntent.putExtra(
                        FusedLocationProviderApi.KEY_LOCATION_CHANGED, location)
                startService(lastLocationIntent)
            }

            // Request new location
            val mLocationRequest = LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            FusedLocationApi.requestLocationUpdates(
                    googleApiClient, mLocationRequest,
                    PendingIntent.getService(this, 0, locationUpdatedIntent, 0))

            googleApiClient.disconnect()
        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.errorCode))
        }
    }

    /**
     * Called when the location has been updated
     */
    private fun locationUpdated(intent: Intent) {
        Log.v(TAG, ACTION_LOCATION_UPDATED)

        // Extra new location
        val location = intent.getParcelableExtra<Location>(FusedLocationProviderApi.KEY_LOCATION_CHANGED)

        if (location != null) {
            val latLngLocation = LatLng(location.latitude, location.longitude)

            // Store in a local preference as well
            Utils.storeLocation(this, latLngLocation)

            // Send a local broadcast so if an Activity is open it can respond
            // to the updated location
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    /**
     * Clears the local device notification
     */
    private fun clearNotificationInternal() {
        Log.v(TAG, ACTION_CLEAR_NOTIFICATION)
        NotificationManagerCompat.from(this).cancel(Constants.MOBILE_NOTIFICATION_ID)
    }

    /**
     * Clears remote device notifications using the Wearable message API
     */
    private fun clearRemoteNotifications() {
        Log.v(TAG, ACTION_CLEAR_REMOTE_NOTIFICATIONS)
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        if (connectionResult.isSuccess && googleApiClient.isConnected) {

            // Loop through all nodes and send a clear notification message
            val itr = Utils.getNodes(googleApiClient).iterator()
            while (itr.hasNext()) {
                Wearable.MessageApi.sendMessage(
                        googleApiClient, itr.next(), Constants.CLEAR_NOTIFICATIONS_PATH, null)
            }
            googleApiClient.disconnect()
        }
    }


    /**
     * Show the notification. Either the regular notification with wearable features
     * added to enhance, or trigger the full micro app on the wearable.

     * @param cityId The city to trigger the notification for
     * *
     * @param microApp If the micro app should be triggered or just enhanced notifications
     */
    private fun showNotification(cityId: String, microApp: Boolean) {

        val attractions = ATTRACTIONS[cityId]

        if (microApp) {
            // If micro app we first need to transfer some data over
            sendDataToWearable(attractions!!)
        }

        // The first (closest) tourist attraction
        val attraction = attractions?.get(0)

        // Limit attractions to send
        val count = if (attractions!!.size > Constants.MAX_ATTRACTIONS)
            Constants.MAX_ATTRACTIONS
        else
            attractions?.size

        // Pull down the tourist attraction images from the network and store
        val bitmaps = HashMap<String, Bitmap>()
        try {
            for (i in 0..count - 1) {
                bitmaps.put(attractions.get(i).name,
                        Glide.with(this)
                                .load(attractions.get(i).imageUrl)
                                .asBitmap()
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(Constants.WEAR_IMAGE_SIZE, Constants.WEAR_IMAGE_SIZE)
                                .get())
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error fetching image from network: " + e)
        } catch (e: ExecutionException) {
            Log.e(TAG, "Error fetching image from network: " + e)
        }

        // The intent to trigger when the notification is tapped
        val pendingIntent = PendingIntent.getActivity(this, 0,
                DetailActivity.getLaunchIntent(this, attraction!!.name),
                PendingIntent.FLAG_UPDATE_CURRENT)

        // The intent to trigger when the notification is dismissed, in this case
        // we want to clear remote notifications as well
        val deletePendingIntent = PendingIntent.getService(this, 0, getClearRemoteNotificationsIntent(this), 0)

        // Construct the main notification
        val builder = NotificationCompat.Builder(this)
                .setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmaps[attraction.name])
                        .setBigContentTitle(attraction.name)
                        .setSummaryText(getString(R.string.nearby_attraction))
                )
                .setLocalOnly(microApp)
                .setContentTitle(attraction!!.name)
                .setContentText(getString(R.string.nearby_attraction))
                .setSmallIcon(R.drawable.ic_stat_maps_pin_drop)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(resources!!.getColor(R.color.colorPrimary, theme))
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)

        if (!microApp) {
            // If not a micro app, create some wearable pages for
            // the other nearby tourist attractions.
            val pages = ArrayList<Notification>()
            for (i in 1..count - 1) {

                // Calculate the distance from current location to tourist attraction
                val distance = Utils.formatDistanceBetween(
                        Utils.getLocation(this), attractions.get(i).location)

                // Construct the notification and add it as a page
                pages.add(NotificationCompat.Builder(this)
                        .setContentTitle(attractions.get(i).name)
                        .setContentText(distance)
                        .setSmallIcon(R.drawable.ic_stat_maps_pin_drop)
                        .extend(NotificationCompat.WearableExtender()
                                .setBackground(bitmaps[attractions.get(i).name])
                        )
                        .build())
            }
            builder.extend(NotificationCompat.WearableExtender().addPages(pages))
        }

        // Trigger the notification
        NotificationManagerCompat.from(this).notify(
                Constants.MOBILE_NOTIFICATION_ID, builder.build())
    }

    /**
     * Transfer the required data over to the wearable
     * @param attractions list of attraction data to transfer over
     */
    private fun sendDataToWearable(attractions: List<Attraction>) {
        val googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build()

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        val connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

        // Limit attractions to send
        val count = if (attractions.size > Constants.MAX_ATTRACTIONS)
            Constants.MAX_ATTRACTIONS
        else
            attractions.size

        val attractionsData = ArrayList<DataMap>(count)

        for (i in 0..count - 1) {
            val attraction = attractions[i]

            var image: Bitmap? = null
            var secondaryImage: Bitmap? = null

            try {
                // Fetch and resize attraction image bitmap
                image = Glide.with(this)
                        .load(attraction.imageUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Constants.WEAR_IMAGE_SIZE_PARALLAX_WIDTH, Constants.WEAR_IMAGE_SIZE)
                        .get()

                secondaryImage = Glide.with(this)
                        .load(attraction.secondaryImageUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Constants.WEAR_IMAGE_SIZE_PARALLAX_WIDTH, Constants.WEAR_IMAGE_SIZE)
                        .get()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Exception loading bitmap from network")
            } catch (e: ExecutionException) {
                Log.e(TAG, "Exception loading bitmap from network")
            }

            if (image != null && secondaryImage != null) {

                val attractionData = DataMap()

                val distance = Utils.formatDistanceBetween(
                        Utils.getLocation(this), attraction.location)

                attractionData.putString(Constants.EXTRA_TITLE, attraction.name)
                attractionData.putString(Constants.EXTRA_DESCRIPTION, attraction.description)
                attractionData.putDouble(
                        Constants.EXTRA_LOCATION_LAT, attraction.location.latitude)
                attractionData.putDouble(
                        Constants.EXTRA_LOCATION_LNG, attraction.location.longitude)
                attractionData.putString(Constants.EXTRA_DISTANCE, distance)
                attractionData.putString(Constants.EXTRA_CITY, attraction.city)
                attractionData.putAsset(Constants.EXTRA_IMAGE,
                        Utils.createAssetFromBitmap(image))
                attractionData.putAsset(Constants.EXTRA_IMAGE_SECONDARY,
                        Utils.createAssetFromBitmap(secondaryImage))

                attractionsData.add(attractionData)
            }
        }

        if (connectionResult.isSuccess && googleApiClient.isConnected
                && attractionsData.size > 0) {

            val dataMap = PutDataMapRequest.create(Constants.ATTRACTION_PATH)
            dataMap.dataMap.putDataMapArrayList(Constants.EXTRA_ATTRACTIONS, attractionsData)
            dataMap.dataMap.putLong(Constants.EXTRA_TIMESTAMP, Date().time)
            val request = dataMap.asPutDataRequest()
            request.setUrgent()

            // Send the data over
            val result = Wearable.DataApi.putDataItem(googleApiClient, request).await()

            if (!result.status.isSuccess) {
                Log.e(TAG, String.format("Error sending data using DataApi (error code = %d)",
                        result.status.statusCode))
            }

        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.errorCode))
        }
        googleApiClient.disconnect()
    }

    companion object {
        private val TAG = UtilityService::class.java.simpleName

        val ACTION_GEOFENCE_TRIGGERED = "geofence_triggered"
        private val ACTION_LOCATION_UPDATED = "location_updated"
        private val ACTION_REQUEST_LOCATION = "request_location"
        private val ACTION_ADD_GEOFENCES = "add_geofences"
        private val ACTION_CLEAR_NOTIFICATION = "clear_notification"
        private val ACTION_CLEAR_REMOTE_NOTIFICATIONS = "clear_remote_notifications"
        private val ACTION_FAKE_UPDATE = "fake_update"
        private val EXTRA_TEST_MICROAPP = "test_microapp"

        val locationUpdatedIntentFilter: IntentFilter
            get() = IntentFilter(UtilityService.ACTION_LOCATION_UPDATED)

        fun triggerWearTest(context: Context, microApp: Boolean) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_FAKE_UPDATE
            intent.putExtra(EXTRA_TEST_MICROAPP, microApp)
            context.startService(intent)
        }

        fun addGeofences(context: Context) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_ADD_GEOFENCES
            context.startService(intent)
        }

        fun requestLocation(context: Context) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_REQUEST_LOCATION
            context.startService(intent)
        }

        fun clearNotification(context: Context) {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_CLEAR_NOTIFICATION
            context.startService(intent)
        }

        fun getClearRemoteNotificationsIntent(context: Context): Intent {
            val intent = Intent(context, UtilityService::class.java)
            intent.action = UtilityService.ACTION_CLEAR_REMOTE_NOTIFICATIONS
            return intent
        }
    }
}
