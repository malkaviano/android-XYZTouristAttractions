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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.android.xyztouristattractions.R

import com.example.android.xyztouristattractions.common.Utils
import com.example.android.xyztouristattractions.service.UtilityService

/**
 * The main tourist attraction activity screen which contains a list of
 * attractions sorted by distance.
 */
class AttractionListActivity :
        AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, AttractionListFragment())
                    .commit()
        }

        // Check fine location permission has been granted
        if (!Utils.checkFineLocationPermission(this)) {
            // See if user has denied permission in the past
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show a simple snackbar explaining the request instead
                showPermissionSnackbar()
            } else {
                // Otherwise request permission from user
                if (savedInstanceState == null) {
                    requestFineLocationPermission()
                }
            }
        } else {
            // Otherwise permission is granted (which is always the case on pre-M devices)
            fineLocationPermissionGranted()
        }
    }

    override fun onResume() {
        super.onResume()

        UtilityService.requestLocation(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.test_notification -> {
                UtilityService.triggerWearTest(this, false)
                showDebugDialog(
                        R.string.action_test_notification,
                        R.string.action_test_notification_dialog
                )
                return true
            }
            R.id.test_microapp -> {
                UtilityService.triggerWearTest(this, true)
                showDebugDialog(
                        R.string.action_test_microapp,
                        R.string.action_test_microapp_dialog
                )
                return true
            }
            R.id.test_toggle_geofence -> {
                val geofenceEnabled = Utils.getGeofenceEnabled(this)
                Utils.storeGeofenceEnabled(this, !geofenceEnabled)
                Toast.makeText(
                        this,
                        "Debug: Geofencing trigger ${
                            if (geofenceEnabled)
                                "disabled"
                            else
                                "enabled"}",
                        Toast.LENGTH_SHORT
                ).show()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Permissions request result callback
     */
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQ -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fineLocationPermissionGranted()
            }
        }
    }

    /**
     * Request the fine location permission from the user
     */
    private fun requestFineLocationPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQ)
    }

    /**
     * Run when fine location permission has been granted
     */
    private fun fineLocationPermissionGranted() {
        UtilityService.addGeofences(this)
        UtilityService.requestLocation(this)
    }

    /**
     * Show a permission explanation snackbar
     */
    private fun showPermissionSnackbar() {
        Snackbar.make(
                findViewById(R.id.container),
                R.string.permission_explanation,
                Snackbar.LENGTH_LONG
                ).setAction(R.string.permission_explanation_action) { requestFineLocationPermission() }
                 .show()
    }

    /**
     * Show a basic debug dialog to provide more info on the built-in debug
     * options.
     */
    private fun showDebugDialog(titleResId: Int, bodyResId: Int) {
        val builder = AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(bodyResId)
                .setPositiveButton(android.R.string.ok, null)
        builder.create().show()
    }

    companion object {

        private val PERMISSION_REQ = 0
    }
}
