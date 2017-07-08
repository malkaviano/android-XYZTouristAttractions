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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
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
import com.example.android.xyztouristattractions.provider.TouristAttractions
import com.example.android.xyztouristattractions.provider.TouristAttractions.ATTRACTIONS
import com.example.android.xyztouristattractions.service.UtilityService
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.*


/**
 * The main tourist attraction fragment which contains a list of attractions
 * sorted by distance (contained inside
 * [com.example.android.xyztouristattractions.ui.AttractionListActivity]).
 */
class AttractionListFragment : Fragment() {

    private lateinit var mAdapter: AttractionAdapter
    private var mLatestLocation: LatLng? = null
    private var mImageSize: Int = 0
    private var mItemClicked: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Load a larger size image to make the activity transition to the detail screen smooth
        mImageSize = resources.getDimensionPixelSize(R.dimen.image_size) * Constants.IMAGE_ANIM_MULTIPLIER

        mLatestLocation = Utils.getLocation(activity)

        val attractions = loadAttractionsFromLocation(mLatestLocation)

        mAdapter = AttractionAdapter(activity, attractions)

        val view = inflater!!.inflate(R.layout.fragment_main, container, false)

        val recyclerView = view.findViewById(android.R.id.list) as AttractionsRecyclerView

        recyclerView.setEmptyView(view.findViewById(android.R.id.empty))
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = mAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        mItemClicked = false
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                mBroadcastReceiver, UtilityService.locationUpdatedIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mBroadcastReceiver)
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            //val location = intent.getParcelableExtra<Location>("com.google.android.location.LOCATION")

            if (LocationResult.hasResult(intent)) {
                val location = LocationResult.extractResult(intent)

                if (location != null) {
                    mLatestLocation = LatLng(location.lastLocation.latitude, location.lastLocation.longitude)
                    mAdapter.mAttractionList = loadAttractionsFromLocation(mLatestLocation)
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun loadAttractionsFromLocation(curLatLng: LatLng?): List<Attraction> {
        val closestCity = TouristAttractions.getClosestCity(curLatLng)

        if (closestCity != null) {
            val attractions = ATTRACTIONS[closestCity]
            if (curLatLng != null) {
                Collections.sort(attractions
                ) { lhs, rhs ->
                    val lhsDistance = SphericalUtil.computeDistanceBetween(
                            lhs.location, curLatLng)
                    val rhsDistance = SphericalUtil.computeDistanceBetween(
                            rhs.location, curLatLng)
                    (lhsDistance - rhsDistance).toInt()
                }
            }

            return attractions!!
        }

        return listOf()
    }

    private inner class AttractionAdapter(
            private val mContext: Context,
            var mAttractionList: List<Attraction>
    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(mContext)
            val view = inflater.inflate(R.layout.list_row, parent, false)
            return ViewHolder(view, this::onItemClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val attraction = mAttractionList[position]

            holder.mTitleTextView.text = attraction.name
            holder.mDescriptionTextView.text = attraction.description

            Glide.with(mContext)
                    .load(attraction.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .placeholder(R.drawable.empty_photo)
                    .override(mImageSize, mImageSize)
                    .into(holder.mImageView)

            val distance = Utils.formatDistanceBetween(mLatestLocation, attraction.location)

            if (TextUtils.isEmpty(distance)) {
                holder.mOverlayTextView.visibility = View.GONE
            } else {
                holder.mOverlayTextView.visibility = View.VISIBLE
                holder.mOverlayTextView.text = distance
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemCount(): Int {
            return mAttractionList.size
        }

        private fun onItemClick(view: View, position: Int) {
            if (!mItemClicked) {
                mItemClicked = true

                val heroView = view.findViewById(android.R.id.icon)

                DetailActivity.launch(
                        activity,
                        mAdapter.mAttractionList[position].name,
                        heroView
                )
            }
        }
    }

    private class ViewHolder(view: View, internal var mItemClickListener: (View, Int) -> Unit) :
            RecyclerView.ViewHolder(view), View.OnClickListener {

        internal var mTitleTextView: TextView
        internal var mDescriptionTextView: TextView
        internal var mOverlayTextView: TextView
        internal var mImageView: ImageView

        init {
            mTitleTextView = view.findViewById(android.R.id.text1) as TextView
            mDescriptionTextView = view.findViewById(android.R.id.text2) as TextView
            mOverlayTextView = view.findViewById(R.id.overlaytext) as TextView
            mImageView = view.findViewById(android.R.id.icon) as ImageView
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            mItemClickListener(v, adapterPosition)
        }
    }
}
