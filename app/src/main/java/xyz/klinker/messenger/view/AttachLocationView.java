/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.FileUtils;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;

/**
 * View for attaching a location as an image.
 */
@SuppressLint("ViewConstructor")
public class AttachLocationView extends FrameLayout implements OnMapReadyCallback,
        LocationListener {

    private FloatingActionButton done;
    private ImageSelectedListener listener;
    private MapFragment mapFragment;
    private GoogleMap map;

    public AttachLocationView(Context context, ImageSelectedListener listener, int color) {
        super(context);

        this.listener = listener;
        init(color);
    }

    private void init(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_attach_location, this, true);

        mapFragment = (MapFragment) ((Activity) getContext()).getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        done = (FloatingActionButton) findViewById(R.id.done);
        done.setBackgroundTintList(ColorStateList.valueOf(color));

        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    map.snapshot(new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {
                            File file = new File(getFileName());
                            FileUtils.writeBitmap(file, bitmap);
                            listener.onImageSelected(Uri.fromFile(file));
                        }
                    });
                }
            }
        });
    }

    private String getFileName() {
        return getContext().getFilesDir() + "/location_" + System.currentTimeMillis() + ".png";
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        try {
            googleMap.setMyLocationEnabled(true);

            LocationManager locationManager = (LocationManager)
                    getContext().getSystemService(Context.LOCATION_SERVICE);

            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager
                    .getBestProvider(criteria, false));
            updateLatLong(location.getLatitude(), location.getLongitude());

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        5000, 10, this);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mapFragment != null && mapFragment.isAdded()) {
            ((Activity) getContext()).getFragmentManager()
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }
    }

    private void updateLatLong(double latitude, double longitude) {
        if (map != null) {
            LatLng position = new LatLng(latitude, longitude);
            MarkerOptions marker = new MarkerOptions().position(position);
            map.addMarker(marker);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position).zoom(17).build();

            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLatLong(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

}
