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

package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.util.FileUtils;
import xyz.klinker.messenger.shared.util.listener.ImageSelectedListener;
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener;

/**
 * View for attaching a location as an image.
 */
@SuppressLint("ViewConstructor")
public class AttachLocationView extends FrameLayout implements OnMapReadyCallback,
        LocationListener {

    private static final String TAG = "AttachLocationView";

    private ImageSelectedListener imageListener;
    private TextSelectedListener textListener;
    private MapFragment mapFragment;
    private GoogleMap map;
    private LocationManager locationManager;
    private double latitude;
    private double longitude;

    public AttachLocationView(Context context, ImageSelectedListener imageListener,
                              TextSelectedListener textListener, int color) {
        super(context);

        this.imageListener = imageListener;
        this.textListener = textListener;
        init(color);
    }

    private void init(int color) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        try {
            inflater.inflate(R.layout.view_attach_location, this, true);
        } catch (Exception e) {
            // this was probably removed already
            return;
        }

        mapFragment = (MapFragment) ((Activity) getContext()).getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton done = (FloatingActionButton) findViewById(R.id.done);
        done.setBackgroundTintList(ColorStateList.valueOf(color));

        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageListener != null) {
                    map.snapshot(new GoogleMap.SnapshotReadyCallback() {
                        @Override
                        public void onSnapshotReady(Bitmap bitmap) {
                            File file = new File(getFileName());
                            FileUtils.INSTANCE.writeBitmap(file, bitmap);
                            imageListener.onImageSelected(Uri.fromFile(file), MimeType.INSTANCE.getIMAGE_JPEG());
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

            locationManager = (LocationManager)
                    getContext().getSystemService(Context.LOCATION_SERVICE);

            Location location = getBestLocationFromEnabledProviders();
            if (location != null) {
                updateLatLong(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(getContext(), R.string.no_location_found, Toast.LENGTH_SHORT).show();
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        5000, 10, this);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Location getBestLocationFromEnabledProviders() throws SecurityException {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);

            if (l == null) continue;
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }

        return bestLocation;
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

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateLatLong(double latitude, double longitude) {
        if (map != null) {
            this.latitude = latitude;
            this.longitude = longitude;

            map.clear();
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
        textListener.onTextSelected(
                "https://maps.google.com/maps/@" + latitude + "," + longitude + ",16z"
        );
//        new Thread(() -> {
//            Log.v(TAG, "getting addresses");
//
//            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
//            List<Address> addresses = null;
//
//            try {
//                addresses = geocoder.getFromLocation(latitude, longitude, 1);
//                Log.v(TAG, "got " + addresses.size() + " addresses");
//            } catch (IOException exception) {
//                Log.e(TAG, "service not available", exception);
//            } catch (IllegalArgumentException exception) {
//                Log.e(TAG, "invalid lat long", exception);
//            }
//
//            if (getHandler() != null) {
//                getHandler().post(() -> textListener.onTextSelected(
//                        "https://maps.google.com/maps/@" + latitude + "," + longitude + ",16z"
//                ));
//            }
//
//            // always attach the link above, instead of trying to find an address, which
//            // didn't seem to work great
//            if (addresses != null && addresses.size() > 0) {
//                Address address = addresses.get(0);
//                ArrayList<String> addressFragments = new ArrayList<>();
//
//                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
//                    addressFragments.add(address.getAddressLine(i));
//                }
//
//                final String a = TextUtils.join(System.getProperty("line.separator"),
//                        addressFragments);
//                Log.v(TAG, "got address: " + a);
//
//                if (getHandler() != null) {
//                    getHandler().post(() -> {
//                        Log.v(TAG, "posting back to fragment");
//                        textListener.onTextSelected(a);
//                    });
//                }
//            } else {
//                Log.e(TAG, "could not find any addresses, using map lat long");
//
//                if (getHandler() != null) {
//                    getHandler().post(() -> textListener.onTextSelected(
//                            "https://maps.google.com/maps/@" + latitude + "," + longitude + ",16z"
//                    ));
//                }
//            }
//        }).start();
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
