package com.example.mainpage.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class LocationPermissionHelper {

    private LocationPermissionHelper() {}

    public static boolean hasForegroundPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasBackgroundPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAllPermissions(@NonNull Context context) {
        return hasForegroundPermission(context) && hasBackgroundPermission(context);
    }

    public static String[] foregroundPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    public static boolean shouldRequestBackgroundPermission(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && hasForegroundPermission(context)
                && !hasBackgroundPermission(context);
    }

    public static void requestBackgroundPermission(@NonNull Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    requestCode
            );
        }
    }
}

