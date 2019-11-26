package io.branch.search.widget.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by sojanpr on 9/20/17.
 * <p>
 * Class for finding the last known location. Handles permission related to location
 * </p>
 */
public final class BranchLocationFinder {
    private static BranchLocationFinder sInstance;
    private static int sInstanceUsers;

    @NonNull
    @SuppressWarnings("UnusedReturnValue")
    public static BranchLocationFinder initialize(@NonNull Activity activity) {
        if (sInstance == null) {
            sInstance = new BranchLocationFinder();
            sInstance.requestLocationUpdate(activity);
        }
        sInstanceUsers++;
        return sInstance;
    }

    @NonNull
    public static BranchLocationFinder getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("Not initialized!");
        }
        return sInstance;
    }

    public static void release() {
        sInstanceUsers--;
        if (sInstanceUsers == 0) {
            sInstance = null;
        }
    }

    private Location mLastKnownLocation = null;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            mLastKnownLocation = locationResult.getLastLocation();
        }
    };

    private BranchLocationFinder() { }

    /**
     * To be called when the activity has updates about the permissions.
     * If location permissions have been granted, this will trigger an update.
     * @param activity activity
     */
    public void requestLocationUpdate(@NonNull final Activity activity) {
        if (!isPermissionAvailable(activity)) return;
        LocationServices.getSettingsClient(activity).checkLocationSettings(
                new LocationSettingsRequest.Builder().addLocationRequest(
                        new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY))
                        .build())
                .addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (mFusedLocationProviderClient != null) {
                            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
                        }
                        final LocationRequest locationRequest = new LocationRequest()
                                .setInterval(1000 * 60 * 2)
                                .setMaxWaitTime(2000);
                            try {
                                mFusedLocationProviderClient
                                        = LocationServices.getFusedLocationProviderClient(activity);
                                mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                                        mLocationCallback,
                                        activity.getMainLooper());
                            } catch (SecurityException ignore) { }
                    }
                }).addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // NOTE: sometimes e is a ResolvableApiException and we should call
                    // e.startResolution! However location permissions are not strictly required
                    // by our API - if not present in the request, we'll geocode the IP address.
                    // So let's not block the user flow here.
                }
            }
        });
    }

    /**
     * Returns the last known location.
     * @return last known or null
     */
    @Nullable
    public Location getLastKnownLocation() {
        return mLastKnownLocation;
    }

    private static boolean isPermissionAvailable(@NonNull Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
