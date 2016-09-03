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
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.FileUtils;
import xyz.klinker.messenger.util.listener.ImageSelectedListener;
import xyz.klinker.messenger.util.listener.TextSelectedListener;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;

/**
 * View for attaching a location as an image.
 */
@SuppressLint("ViewConstructor")
public class AttachLocationView extends FrameLayout implements OnMapReadyCallback,
        LocationListener {

    private FloatingActionButton done;
    private ImageSelectedListener imageListener;
    private TextSelectedListener textListener;
    private MapFragment mapFragment;
    private GoogleMap map;
    private double latitude;
    private double longitude;

    public AttachLocationView(Context context, ImageSelectedListener imageListener,
                              TextSelectedListener textListener, int color) {
        super(context);

        this.imageListener = imageListener;
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
                if (imageListener != null) {
                    map.snapshot(new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {
                            File file = new File(getFileName());
                            FileUtils.writeBitmap(file, bitmap);
                            imageListener.onImageSelected(Uri.fromFile(file));
                        }
                    });
                }

                if (textListener != null) {
                    attachAddress();
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
            this.latitude = latitude;
            this.longitude = longitude;

            LatLng position = new LatLng(latitude, longitude);
            MarkerOptions marker = new MarkerOptions().position(position);
            map.addMarker(marker);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position).zoom(17).build();

            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }
    }

    private void attachAddress() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocation(latitude, longitude, 1);
                } catch (IOException exception) {
                    Log.e(TAG, "service not available", exception);
                } catch (IllegalArgumentException exception) {
                    Log.e(TAG, "invalid lat long", exception);
                }

                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    final ArrayList<String> addressFragments = new ArrayList<>();

                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addressFragments.add(address.getAddressLine(i));
                    }

                    getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            textListener.onTextSelected(
                                    TextUtils.join(System.getProperty("line.separator"),
                                    addressFragments));
                        }
                    });
                } else {
                    Log.e(TAG, "could not find any addresses");
                }
            }
        }).start();
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

    private final class Constants {
        public static final int SUCCESS_RESULT = 0;
        public static final int FAILURE_RESULT = 1;
        public static final String PACKAGE_NAME =
                "com.google.android.gms.location.sample.locationaddress";
        public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
        public static final String RESULT_DATA_KEY = PACKAGE_NAME +
                ".RESULT_DATA_KEY";
        public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
                ".LOCATION_DATA_EXTRA";
    }

}
