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

package com.example.android.xyztouristattractions.ui

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Attraction
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.common.Utils

import com.example.android.xyztouristattractions.provider.TouristAttractions.ATTRACTIONS

/**
 * The tourist attraction detail fragment which contains the details of a
 * a single attraction (contained inside
 * [com.example.android.xyztouristattractions.ui.DetailActivity]).
 */
class DetailFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_detail, container, false)

        val attractionName = arguments.getString(EXTRA_ATTRACTION)

        val attraction = findAttraction(attractionName)

        if (attraction == null) {
            activity.finish()

            return null
        }

        val nameTextView = view.findViewById(R.id.nameTextView) as TextView
        val descTextView = view.findViewById(R.id.descriptionTextView) as TextView
        val distanceTextView = view.findViewById(R.id.distanceTextView) as TextView
        val imageView = view.findViewById(R.id.imageView) as ImageView
        val mapFab = view.findViewById(R.id.mapFab) as FloatingActionButton

        val location = Utils.getLocation(activity)
        val distance = Utils.formatDistanceBetween(location, attraction.location)

        if (TextUtils.isEmpty(distance)) {
            distanceTextView.visibility = View.GONE
        }

        nameTextView.text = attractionName
        distanceTextView.text = distance
        descTextView.text = attraction.longDescription

        val imageSize = resources.getDimensionPixelSize(R.dimen.image_size) * Constants.IMAGE_ANIM_MULTIPLIER

        Glide.with(activity)
                .load(attraction.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .placeholder(R.color.lighter_gray)
                .override(imageSize, imageSize)
                .into(imageView)

        mapFab.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse(
                    Constants.MAPS_INTENT_URI + Uri.encode("${attraction.name}, ${attraction.city}")
            )

            //startActivity(intent)

            // Verify that the intent will resolve to an activity
            if (intent.resolveActivity(activity.packageManager) != null) {
                startActivity(intent)
            }
        }

        return view
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Some small additions to handle "up" navigation correctly
                val upIntent = NavUtils.getParentActivityIntent(activity)
                upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Check if up activity needs to be created (usually when
                // detail screen is opened from a notification or from the
                // Wearable app
                if (NavUtils.shouldUpRecreateTask(activity, upIntent) || activity.isTaskRoot) {

                    // Synthesize parent stack
                    TaskStackBuilder.create(activity)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // On Lollipop+ we finish so to run the nice animation
                    activity.finishAfterTransition()

                    return true
                }

                // Otherwise let the system handle navigating "up"
                return false
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Really hacky loop for finding attraction in our static content provider.
     * Obviously would not be used in a production app.
     */
    private fun findAttraction(attractionName: String): Attraction? {
        for ((_, attractions) in ATTRACTIONS) {

            attractions.forEach {
                if (attractionName == it.name) {
                    return it
                }
            }
        }

        return null
    }

    companion object {

        private val EXTRA_ATTRACTION = "attraction"

        fun createInstance(attractionName: String): DetailFragment {
            val detailFragment = DetailFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_ATTRACTION, attractionName)
            detailFragment.arguments = bundle
            return detailFragment
        }
    }
}
