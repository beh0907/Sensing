package com.coretec.sensing.utils;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class GpsTracker extends Service implements LocationListener {

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

    private final Context context;

    private LocationManager locationManager;

    private int updateTime;

    private Location location;
    private double latitude;
    private double longitude;
    private double altitude;


    public GpsTracker(Context context, int updateTime) {
        this.context = context;
        this.updateTime = updateTime;
        getLocation();
    }


    public void getLocation() {
        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {

            } else {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION);


                if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                        hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

                    ;
                } else
                    return;


                if (isNetworkEnabled) {


                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateTime, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }


                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateTime, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("@@@", "" + e.toString());
        }

    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        } else {
            latitude = 0;
        }

        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        } else {
            longitude = 0;
        }

        return longitude;
    }

    public double getAltitude() {
        if (location != null) {
            altitude = location.getAltitude();
        } else {
            altitude = 0;
        }

        return altitude;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    public void stop() {
        if (locationManager != null) {
            locationManager.removeUpdates(GpsTracker.this);
        }
    }
}