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

import android.graphics.Bitmap
import android.net.Uri

import com.google.android.gms.maps.model.LatLng

/**
 * A simple shared tourist attraction class to easily pass data around. Used
 * in both the mobile app and wearable app.
 */
data class Attraction(
    var name: String = "",
    var description: String = "",
    var longDescription: String = "",
    var imageUrl: Uri = Uri.EMPTY,
    var secondaryImageUrl: Uri = Uri.EMPTY,
    var location: LatLng = LatLng(0.0, 0.0),
    var city: String = "",
    var image: Bitmap? = null,
    var secondaryImage: Bitmap? = null,
    var distance: String? = null
)