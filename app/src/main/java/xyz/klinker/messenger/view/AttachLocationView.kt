/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.util.FileUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener
import java.io.File

/**
 * View for attaching a location as an image.
 */
@SuppressLint("ViewConstructor")
class AttachLocationView(context: Context, private val imageListener: ImageSelectedListener?,
                         private val textListener: TextSelectedListener?, color: Int)

    : FrameLayout(context), OnMapReadyCallback, LocationListener {

    private val mapFragment: MapFragment by lazy { (context as Activity).fragmentManager.findFragmentById(R.id.map) as MapFragment }
    private var map: GoogleMap? = null
    private var locationManager: LocationManager? = null
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private val fileName: String
        get() = context.filesDir.toString() + "/location_" + TimeUtils.now + ".png"

    private val bestLocationFromEnabledProviders: Location?
        @Throws(SecurityException::class)
        get() {
            val providers = locationManager!!.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val l = locationManager!!.getLastKnownLocation(provider) ?: continue

                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            return bestLocation
        }

    init {
        try {
            LayoutInflater.from(context).inflate(R.layout.view_attach_location, this, true)
            mapFragment.getMapAsync(this)

            val done = findViewById<View>(R.id.done) as FloatingActionButton
            done.backgroundTintList = ColorStateList.valueOf(color)

            done.setOnClickListener {
                if (imageListener != null) {
                    map?.snapshot { bitmap ->
                        val file = File(fileName)
                        FileUtils.writeBitmap(file, bitmap)
                        imageListener.onImageSelected(Uri.fromFile(file), MimeType.IMAGE_JPEG)
                    }
                }

                if (textListener != null) {
                    attachAddress()
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap

        try {
            googleMap.isMyLocationEnabled = true

            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location = bestLocationFromEnabledProviders
            if (location != null) {
                updateLatLong(location.latitude, location.longitude)
            } else {
                Toast.makeText(context, R.string.no_location_found, Toast.LENGTH_SHORT).show()
            }

            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        5000, 10f, this)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (mapFragment.isAdded) {
            (context as Activity).fragmentManager
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit()
        }

        if (locationManager != null) {
            try {
                locationManager!!.removeUpdates(this)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLatLong(latitude: Double, longitude: Double) {
        if (map != null) {
            this.latitude = latitude
            this.longitude = longitude

            map?.clear()
            val position = LatLng(latitude, longitude)
            val marker = MarkerOptions().position(position)
            map?.addMarker(marker)

            val cameraPosition = CameraPosition.Builder()
                    .target(position).zoom(17f).build()

            map?.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition))
        }
    }

    private fun attachAddress() {
        textListener?.onTextSelected("https://maps.google.com/maps/@$latitude,$longitude,16z")
    }

    override fun onLocationChanged(location: Location) {
        updateLatLong(location.latitude, location.longitude)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
    override fun onProviderEnabled(provider: String?) { }
    override fun onProviderDisabled(provider: String?) { }
}
