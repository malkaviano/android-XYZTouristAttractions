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

package com.example.android.xyztouristattractions.common

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Display

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeApi
import com.google.android.gms.wearable.Wearable
import com.google.maps.android.SphericalUtil

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.NumberFormat
import java.util.HashSet

/**
 * This class contains shared static utility methods that both the mobile and
 * wearable apps can use.
 */
object Utils {
    private val TAG = Utils::class.java.simpleName

    private val PREFERENCES_LAT = "lat"
    private val PREFERENCES_LNG = "lng"
    private val PREFERENCES_GEOFENCE_ENABLED = "geofence"
    private val DISTANCE_KM_POSTFIX = "km"
    private val DISTANCE_M_POSTFIX = "m"

    /**
     * Check if the app has access to fine location permission. On pre-M
     * devices this will always return true.
     */
    fun checkFineLocationPermission(context: Context): Boolean {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Calculate distance between two LatLng points and format it nicely for
     * display. As this is a sample, it only statically supports metric units.
     * A production app should check locale and support the correct units.
     */
    fun formatDistanceBetween(point1: LatLng?, point2: LatLng?): String? {
        if (point1 == null || point2 == null) {
            return null
        }

        val numberFormat = NumberFormat.getNumberInstance()
        val distance = Math.round(SphericalUtil.computeDistanceBetween(point1, point2)).toDouble()

        // Adjust to KM if M goes over 1000 (see javadoc of method for note
        // on only supporting metric)
        if (distance >= 1000) {
            numberFormat.maximumFractionDigits = 1
            return numberFormat.format(distance / 1000) + DISTANCE_KM_POSTFIX
        }
        return numberFormat.format(distance) + DISTANCE_M_POSTFIX
    }

    /**
     * Store the location in the app preferences.
     */
    fun storeLocation(context: Context, location: LatLng) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putLong(PREFERENCES_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
        editor.putLong(PREFERENCES_LNG, java.lang.Double.doubleToRawLongBits(location.longitude))
        editor.apply()
    }

    /**
     * Fetch the location from app preferences.
     */
    fun getLocation(context: Context): LatLng? {
        if (!checkFineLocationPermission(context)) {
            return null
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lat = prefs.getLong(PREFERENCES_LAT, java.lang.Long.MAX_VALUE)
        val lng = prefs.getLong(PREFERENCES_LNG, java.lang.Long.MAX_VALUE)
        if (lat !== java.lang.Long.MAX_VALUE && lng !== java.lang.Long.MAX_VALUE) {
            val latDbl = java.lang.Double.longBitsToDouble(lat)
            val lngDbl = java.lang.Double.longBitsToDouble(lng)
            return LatLng(latDbl, lngDbl)
        }
        return null
    }

    /**
     * Store if geofencing triggers will show a notification in app preferences.
     */
    fun storeGeofenceEnabled(context: Context, enable: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putBoolean(PREFERENCES_GEOFENCE_ENABLED, enable)
        editor.apply()
    }

    /**
     * Retrieve if geofencing triggers should show a notification from app preferences.
     */
    fun getGeofenceEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREFERENCES_GEOFENCE_ENABLED, true)
    }

    /**
     * Convert an asset into a bitmap object synchronously. Only call this
     * method from a background thread (it should never be called from the
     * main/UI thread as it blocks).
     */
    fun loadBitmapFromAsset(googleApiClient: GoogleApiClient, asset: Asset?): Bitmap? {
        if (asset == null) {
            throw IllegalArgumentException("Asset must be non-null")
        }
        // convert asset into a file descriptor and block until it's ready
        val assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().inputStream

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.")
            return null
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream)
    }

    /**
     * Create a wearable asset from a bitmap.
     */
    fun createAssetFromBitmap(bitmap: Bitmap?): Asset? {
        if (bitmap != null) {
            val byteStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
            return Asset.createFromBytes(byteStream.toByteArray())
        }
        return null
    }

    /**
     * Get a list of all wearable nodes that are connected synchronously.
     * Only call this method from a background thread (it should never be
     * called from the main/UI thread as it blocks).
     */
    fun getNodes(client: GoogleApiClient): Collection<String> {
        val results = HashSet<String>()
        val nodes = Wearable.NodeApi.getConnectedNodes(client).await()
        for (node in nodes.nodes) {
            results.add(node.id)
        }
        return results
    }

    /**
     * Calculates the square insets on a round device. If the system insets are not set
     * (set to 0) then the inner square of the circle is applied instead.

     * @param display device default display
     * *
     * @param systemInsets the system insets
     * *
     * @return adjusted square insets for use on a round device
     */
    fun calculateBottomInsetsOnRoundDevice(display: Display, systemInsets: Rect): Rect {
        val size = Point()
        display.getSize(size)
        val width = size.x + systemInsets.left + systemInsets.right
        val height = size.y + systemInsets.top + systemInsets.bottom

        // Minimum inset to use on a round screen, calculated as a fixed percent of screen height
        val minInset = (height * Constants.WEAR_ROUND_MIN_INSET_PERCENT).toInt()

        // Use system inset if it is larger than min inset, otherwise use min inset
        val bottomInset = if (systemInsets.bottom > minInset) systemInsets.bottom else minInset

        // Calculate left and right insets based on bottom inset
        val radius = (width / 2).toDouble()
        val apothem = radius - bottomInset
        val chord = Math.sqrt(Math.pow(radius, 2.0) - Math.pow(apothem, 2.0)) * 2
        val leftRightInset = ((width - chord) / 2).toInt()

        Log.d(TAG, "calculateBottomInsetsOnRoundDevice: $bottomInset, $leftRightInset")

        return Rect(leftRightInset, 0, leftRightInset, bottomInset)
    }
}
