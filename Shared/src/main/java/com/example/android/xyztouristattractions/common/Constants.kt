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

object Constants {

    // Set to false to have the geofence trigger use the enhanced notifications instead
    val USE_MICRO_APP = true

    val GOOGLE_API_CLIENT_TIMEOUT_S = 10 // 10 seconds
    val GOOGLE_API_CLIENT_ERROR_MSG = "Failed to connect to GoogleApiClient (error code = %d)"

    // Used to size the images in the mobile app so they can animate cleanly from list to detail
    val IMAGE_ANIM_MULTIPLIER = 2

    // Resize images sent to Wear to 400x400px
    val WEAR_IMAGE_SIZE = 400

    // Except images that can be set as a background with parallax, set width 640x instead
    val WEAR_IMAGE_SIZE_PARALLAX_WIDTH = 640

    // The minimum bottom inset percent to use on a round screen device
    val WEAR_ROUND_MIN_INSET_PERCENT = 0.08f

    // Max # of attractions to show at once
    val MAX_ATTRACTIONS = 4

    // Notification IDs
    val MOBILE_NOTIFICATION_ID = 100
    val WEAR_NOTIFICATION_ID = 200

    // Intent and bundle extras
    val EXTRA_ATTRACTIONS = "extra_attractions"
    val EXTRA_ATTRACTIONS_URI = "extra_attractions_uri"
    val EXTRA_TITLE = "extra_title"
    val EXTRA_DESCRIPTION = "extra_description"
    val EXTRA_LOCATION_LAT = "extra_location_lat"
    val EXTRA_LOCATION_LNG = "extra_location_lng"
    val EXTRA_DISTANCE = "extra_distance"
    val EXTRA_CITY = "extra_city"
    val EXTRA_IMAGE = "extra_image"
    val EXTRA_IMAGE_SECONDARY = "extra_image_secondary"
    val EXTRA_TIMESTAMP = "extra_timestamp"

    // Wear Data API paths
    val ATTRACTION_PATH = "/attraction"
    val START_PATH = "/start"
    val START_ATTRACTION_PATH = START_PATH + "/attraction"
    val START_NAVIGATION_PATH = START_PATH + "/navigation"
    val CLEAR_NOTIFICATIONS_PATH = "/clear"

    // Maps values
    val MAPS_INTENT_URI = "geo:0,0?q="
    val MAPS_NAVIGATION_INTENT_URI = "google.navigation:mode=w&q="

}
