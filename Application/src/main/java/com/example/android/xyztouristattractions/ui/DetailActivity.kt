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

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.example.android.xyztouristattractions.R

/**
 * The tourist attraction detail activity screen which contains the details of
 * a single attraction.
 */
class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val attraction = intent.getStringExtra(EXTRA_ATTRACTION)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, DetailFragment.createInstance(attraction))
                    .commit()
        }
    }

    companion object {

        private val EXTRA_ATTRACTION = "attraction"

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun launch(activity: Activity, attraction: String, heroView: View) {
            val intent = getLaunchIntent(activity, attraction)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity, heroView, heroView.transitionName)
                ActivityCompat.startActivity(activity, intent, options.toBundle())
            } else {
                activity.startActivity(intent)
            }
        }

        fun getLaunchIntent(context: Context, attraction: String): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(EXTRA_ATTRACTION, attraction)
            return intent
        }
    }
}
