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
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.wearable.activity.ConfirmationActivity
import android.support.wearable.view.ActionPage
import android.support.wearable.view.CardFrame
import android.support.wearable.view.CardScrollView
import android.support.wearable.view.GridPagerAdapter
import android.support.wearable.view.GridViewPager
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import com.example.android.xyztouristattractions.R
import com.example.android.xyztouristattractions.common.Attraction
import com.example.android.xyztouristattractions.common.Constants
import com.example.android.xyztouristattractions.service.UtilityService

import java.util.ArrayList

/**
 * This adapter backs the main GridViewPager component found in
 * [com.example.android.xyztouristattractions.ui.AttractionsActivity].
 */
class AttractionsGridPagerAdapter(
        private val mContext: Context, private var mAttractions: ArrayList<Attraction>?) : GridPagerAdapter(), GridViewPager.OnPageChangeListener {
    private val mLayoutInflater: LayoutInflater
    private var mInsets = Rect()
    private val mDelayedHide = DelayedHide()
    private var mOnChromeFadeListener: OnChromeFadeListener? = null

    init {
        mLayoutInflater = LayoutInflater.from(mContext)
    }

    fun setData(attractions: ArrayList<Attraction>) {
        mAttractions = attractions
    }

    fun setInsets(insets: Rect) {
        mInsets = insets
    }

    override fun getRowCount(): Int {
        return if (mAttractions != null && mAttractions!!.size > 0) mAttractions!!.size else 1
    }

    override fun getColumnCount(i: Int): Int {
        return GRID_COLUMN_COUNT
    }

    override fun instantiateItem(container: ViewGroup, row: Int, column: Int): Any {
        if (mAttractions != null && mAttractions!!.size > 0) {
            val attraction = mAttractions!![row]
            when (column) {
                PAGER_PRIMARY_IMAGE_COLUMN, PAGER_SECONDARY_IMAGE_COLUMN -> {
                    // Two pages of full screen images, one with the attraction name
                    // and one with the distance to the attraction
                    val view = mLayoutInflater.inflate(
                            R.layout.gridpager_fullscreen_image, container, false)
                    val imageView = view.findViewById(R.id.imageView) as ImageView
                    val textView = view.findViewById(R.id.textView) as TextView
                    val overlayTextLayout = view.findViewById(R.id.overlaytext) as FrameLayout

                    mDelayedHide.add(overlayTextLayout)
                    view.setOnClickListener(mDelayedHide)

                    val params = textView.layoutParams as FrameLayout.LayoutParams
                    params.bottomMargin = params.bottomMargin + mInsets.bottom
                    params.leftMargin = mInsets.left
                    params.rightMargin = mInsets.right
                    textView.layoutParams = params

                    if (column == PAGER_PRIMARY_IMAGE_COLUMN) {
                        imageView.setImageBitmap(attraction.image)
                        textView.text = attraction.name
                    } else {
                        imageView.setImageBitmap(attraction.secondaryImage)
                        if (TextUtils.isEmpty(attraction.distance)) {
                            overlayTextLayout.visibility = View.GONE
                        } else {
                            textView.text = mContext.getString(
                                    R.string.map_caption, attraction.distance)
                        }
                    }
                    container.addView(view)
                    return view
                }
                PAGER_DESCRIPTION_COLUMN -> {
                    // The description card page
                    val cardScrollView = mLayoutInflater.inflate(
                            R.layout.gridpager_card, container, false) as CardScrollView
                    val descTextView = cardScrollView.findViewById(R.id.textView) as TextView
                    descTextView.text = attraction.description
                    cardScrollView.cardGravity = Gravity.BOTTOM
                    cardScrollView.isExpansionEnabled = true
                    cardScrollView.expansionDirection = CardFrame.EXPAND_DOWN
                    cardScrollView.expansionFactor = 10f
                    container.addView(cardScrollView)
                    return cardScrollView
                }
                PAGER_NAVIGATE_ACTION_COLUMN -> {
                    // The navigate action
                    val navActionPage = mLayoutInflater.inflate(
                            R.layout.gridpager_action, container, false) as ActionPage

                    navActionPage.setOnClickListener(getStartActionClickListener(
                            attraction, Constants.START_NAVIGATION_PATH,
                            ConfirmationActivity.SUCCESS_ANIMATION))
                    navActionPage.setImageResource(R.drawable.ic_full_directions_walking)
                    navActionPage.setText(mContext.getString(R.string.action_navigate))

                    container.addView(navActionPage)
                    return navActionPage
                }
                PAGER_OPEN_ACTION_COLUMN -> {
                    // The "open on device" action
                    val openActionPage = mLayoutInflater.inflate(
                            R.layout.gridpager_action, container, false) as ActionPage

                    openActionPage.setOnClickListener(getStartActionClickListener(
                            attraction, Constants.START_ATTRACTION_PATH,
                            ConfirmationActivity.OPEN_ON_PHONE_ANIMATION))
                    openActionPage.setImageResource(R.drawable.ic_full_openonphone)
                    openActionPage.setText(mContext.getString(R.string.action_open))

                    container.addView(openActionPage)
                    return openActionPage
                }
            }
        }
        return View(mContext)
    }

    override fun getBackgroundForPage(row: Int, column: Int): Drawable {
        if (column == 0) {
            return ColorDrawable(0) // Empty black drawable
        }
        if (mAttractions!!.size > 0 && mAttractions!![row].image != null) {
            return BitmapDrawable(mContext.resources, mAttractions!![row].image)
        }
        return super.getBackgroundForPage(row, column)
    }

    override fun destroyItem(viewGroup: ViewGroup, row: Int, column: Int, `object`: Any) {
        mDelayedHide.remove(`object` as View)
        viewGroup.removeView(`object`)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun onPageScrolled(posX: Int, posY: Int, posOffsetX: Float, posOffsetY: Float,
                                posOffsetPixelsX: Int, posOffsetPixelsY: Int) {
    }

    override fun onPageSelected(row: Int, col: Int) {}

    override fun onPageScrollStateChanged(state: Int) {
        mDelayedHide.show()
    }

    /**
     * Use the Wear Message API to execute an action. Clears local and remote notifications and
     * also runs a confirmation animation before finishing the Wear activity.

     * @param attraction The attraction to start the action on
     * *
     * @param pathName The Wear Message API pathname
     * *
     * @param confirmAnimationType The confirmation animation type from ConfirmationActivity
     */
    private fun startAction(attraction: Attraction, pathName: String, confirmAnimationType: Int) {
        val intent = Intent(mContext, ConfirmationActivity::class.java)
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, confirmAnimationType)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        mContext.startActivity(intent)

        UtilityService.clearNotification(mContext)
        UtilityService.clearRemoteNotifications(mContext)
        UtilityService.startDeviceActivity(mContext, pathName, attraction.name, attraction.city)

        (mContext as Activity).finish()
    }

    /**
     * Helper method to generate the OnClickListener for the attraction actions.
     */
    private fun getStartActionClickListener(attraction: Attraction,
                                            pathName: String, confirmAnimationType: Int): View.OnClickListener {
        val clickListener = View.OnClickListener { startAction(attraction, pathName, confirmAnimationType) }
        return clickListener
    }

    fun setOnChromeFadeListener(listener: OnChromeFadeListener) {
        mOnChromeFadeListener = listener
    }

    interface OnChromeFadeListener {
        fun onChromeFadeIn()
        fun onChromeFadeOut()
    }

    /**
     * Helper class to fade out views based on a delay and fade them back in if needed as well.
     */
    private inner class DelayedHide : View.OnClickListener {

        internal var hideViews = ArrayList<View>(GRID_COLUMN_COUNT)
        internal var mHideHandler = Handler()
        internal var mIsHidden = false

        internal var mHideRunnable: Runnable = Runnable { hide() }

        internal fun add(newView: View) {
            hideViews.add(newView)
            delayedHide()
        }

        internal fun remove(removeView: View) {
            hideViews.remove(removeView)
        }

        internal fun show() {
            mIsHidden = false
            if (mOnChromeFadeListener != null) {
                mOnChromeFadeListener!!.onChromeFadeIn()
            }
            for (view in hideViews) {
                view?.animate()?.alpha(1f)?.setDuration(FADE_IN_TIME_MS.toLong())?.start()
            }
            delayedHide()
        }

        internal fun hide() {
            mIsHidden = true
            mHideHandler.removeCallbacks(mHideRunnable)
            if (mOnChromeFadeListener != null) {
                mOnChromeFadeListener!!.onChromeFadeOut()
            }
            for (i in hideViews.indices) {
                if (hideViews[i] != null) {
                    hideViews[i].animate().alpha(0f).setDuration(FADE_OUT_TIME_MS.toLong()).start()
                }
            }
        }

        internal fun delayedHide() {
            mHideHandler.removeCallbacks(mHideRunnable)
            mHideHandler.postDelayed(mHideRunnable, FADE_OUT_DELAY_MS.toLong())
        }

        override fun onClick(v: View) {
            if (mIsHidden) {
                show()
            } else {
                hide()
            }
        }
    }

    companion object {

        val FADE_IN_TIME_MS = 250
        val FADE_OUT_TIME_MS = 500
        private val GRID_COLUMN_COUNT = 5
        private val FADE_OUT_DELAY_MS = 1500
        private val PAGER_PRIMARY_IMAGE_COLUMN = 0
        private val PAGER_SECONDARY_IMAGE_COLUMN = 1
        private val PAGER_DESCRIPTION_COLUMN = 2
        private val PAGER_NAVIGATE_ACTION_COLUMN = 3
        private val PAGER_OPEN_ACTION_COLUMN = 4
    }
}
