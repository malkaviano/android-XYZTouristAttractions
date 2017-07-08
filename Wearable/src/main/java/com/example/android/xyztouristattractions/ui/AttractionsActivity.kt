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

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.wearable.view.DismissOverlayView
import android.support.wearable.view.DotsPageIndicator
import android.support.wearable.view.GridViewPager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ProgressBar

import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Attraction
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.common.Utils
import com.example.android.xyztouristattractions.service.UtilityService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * The main Wear activity that displays nearby attractions in a
 * [android.support.wearable.view.GridViewPager]. Each row shows
 * one attraction and each column shows information or actions for that
 * particular attraction.
 */
class AttractionsActivity : Activity(), AttractionsGridPagerAdapter.OnChromeFadeListener {

    private var mGestureDetector: GestureDetectorCompat? = null
    private var mDismissOverlayView: DismissOverlayView? = null
    private var mGridViewPager: GridViewPager? = null
    private var mAdapter: AttractionsGridPagerAdapter? = null
    private var mDotsPageIndicator: DotsPageIndicator? = null
    private var mProgressBar: ProgressBar? = null
    private var mInsets = Rect(0, 0, 0, 0)

    private val mAttractions = ArrayList<Attraction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val topFrameLayout = findViewById(R.id.topFrameLayout) as FrameLayout
        mProgressBar = findViewById(R.id.progressBar) as ProgressBar
        mGridViewPager = findViewById(R.id.gridViewPager) as GridViewPager
        mDotsPageIndicator = findViewById(R.id.dotsPageIndicator) as DotsPageIndicator
        mAdapter = AttractionsGridPagerAdapter(this, mAttractions)
        mAdapter!!.setOnChromeFadeListener(this)
        mGridViewPager!!.adapter = mAdapter
        mDotsPageIndicator!!.setPager(mGridViewPager)
        mDotsPageIndicator!!.setOnPageChangeListener(mAdapter)

        topFrameLayout.setOnApplyWindowInsetsListener { v, insets ->
            var insets = insets
            // Call through to super implementation
            insets = topFrameLayout.onApplyWindowInsets(insets)

            val round = insets.isRound

            // Store system window insets regardless of screen shape
            mInsets.set(insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom)

            if (round) {
                // On a round screen calculate the square inset to use.
                // Alternatively could use BoxInsetLayout, although calculating
                // the inset ourselves lets us position views outside the center
                // box. For example, slightly lower on the round screen (by giving
                // up some horizontal space).
                mInsets = Utils.calculateBottomInsetsOnRoundDevice(
                        windowManager.defaultDisplay, mInsets)

                // Boost the dots indicator up by the bottom inset
                val params = mDotsPageIndicator!!.layoutParams as FrameLayout.LayoutParams
                params.bottomMargin = mInsets.bottom
                mDotsPageIndicator!!.layoutParams = params
            }

            mAdapter!!.setInsets(mInsets)
            insets
        }

        // Set up the DismissOverlayView
        mDismissOverlayView = findViewById(R.id.dismiss_overlay) as DismissOverlayView
        mDismissOverlayView!!.setIntroText(getString(R.string.exit_intro_text))
        mDismissOverlayView!!.showIntroIfNecessary()
        mGestureDetector = GestureDetectorCompat(this, LongPressListener())

        val attractionsUri = intent.getParcelableExtra<Uri>(Constants.EXTRA_ATTRACTIONS_URI)
        if (attractionsUri != null) {
            FetchDataAsyncTask(this).execute(attractionsUri)
            UtilityService.clearNotification(this)
            UtilityService.clearRemoteNotifications(this)
        } else {
            finish()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector!!.onTouchEvent(event) || super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector!!.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onChromeFadeIn() {
        // As the custom UI chrome fades in, also fade the DotsPageIndicator in
        mDotsPageIndicator!!.animate().alpha(1f).setDuration(
                AttractionsGridPagerAdapter.FADE_IN_TIME_MS.toLong()).start()
    }

    override fun onChromeFadeOut() {
        // As the custom UI chrome fades out, also fade the DotsPageIndicator out
        mDotsPageIndicator!!.animate().alpha(0f).setDuration(
                AttractionsGridPagerAdapter.FADE_OUT_TIME_MS.toLong()).start()
    }

    private inner class LongPressListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(event: MotionEvent) {
            mDismissOverlayView!!.show()
        }
    }

    /**
     * A background task to load the attraction data via the Wear DataApi.
     * This can take a second or two sometimes as several images need to
     * be loaded.
     */
    private inner class FetchDataAsyncTask(private val mContext: Context) : AsyncTask<Uri, Void, ArrayList<Attraction>>() {

        override fun doInBackground(vararg params: Uri): ArrayList<Attraction>? {
            mAttractions.clear()

            // Connect to Play Services and the Wearable API
            val googleApiClient = GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .build()

            val connectionResult = googleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT_S.toLong(), TimeUnit.SECONDS)

            if (!connectionResult.isSuccess || !googleApiClient.isConnected) {
                Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                        connectionResult.errorCode))
                return null
            }

            val attractionsUri = params[0]
            val dataItemResult = Wearable.DataApi.getDataItem(googleApiClient, attractionsUri).await()

            if (dataItemResult.status.isSuccess && dataItemResult.dataItem != null) {
                val dataMapItem = DataMapItem.fromDataItem(dataItemResult.dataItem)
                val attractionsData = dataMapItem.dataMap.getDataMapArrayList(Constants.EXTRA_ATTRACTIONS)

                // Loop through each attraction, adding them to the list
                val itr = attractionsData.iterator()
                while (itr.hasNext()) {
                    val attractionData = itr.next()

                    val attraction = Attraction()
                    attraction.name = attractionData.getString(Constants.EXTRA_TITLE)
                    attraction.description = attractionData.getString(Constants.EXTRA_DESCRIPTION)
                    attraction.city = attractionData.getString(Constants.EXTRA_CITY)
                    attraction.distance = attractionData.getString(Constants.EXTRA_DISTANCE)
                    attraction.location = LatLng(
                            attractionData.getDouble(Constants.EXTRA_LOCATION_LAT),
                            attractionData.getDouble(Constants.EXTRA_LOCATION_LNG))
                    attraction.image = Utils.loadBitmapFromAsset(googleApiClient,
                            attractionData.getAsset(Constants.EXTRA_IMAGE))
                    attraction.secondaryImage = Utils.loadBitmapFromAsset(googleApiClient,
                            attractionData.getAsset(Constants.EXTRA_IMAGE_SECONDARY))

                    mAttractions.add(attraction)
                }
            }

            googleApiClient.disconnect()

            return mAttractions
        }

        override fun onPostExecute(result: ArrayList<Attraction>?) {
            if (result != null && result.size > 0) {
                // Update UI based on the result of the background processing
                mAdapter!!.setData(result)
                mAdapter!!.notifyDataSetChanged()
                mProgressBar!!.visibility = View.GONE
                mDotsPageIndicator!!.visibility = View.VISIBLE
                mGridViewPager!!.visibility = View.VISIBLE
            } else {
                finish()
            }
        }
    }

    companion object {
        private val TAG = AttractionsActivity::class.java.simpleName
    }
}
