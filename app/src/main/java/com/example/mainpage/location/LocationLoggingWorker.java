package com.example.mainpage.location;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.TimeUnit;

public class LocationLoggingWorker extends Worker {

    private static final long LOCATION_TIMEOUT_SECONDS = 30;
    private static final long DATA_RETENTION_MILLIS = TimeUnit.DAYS.toMillis(7);

    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationLogDao locationLogDao;

    public LocationLoggingWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationLogDao = LocationDatabase.getInstance(context).locationLogDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();

        if (!LocationPermissionHelper.hasForegroundPermission(appContext)) {
            LocationWorkScheduler.scheduleNextWork(appContext);
            return Result.success();
        }

        if (!LocationWorkScheduler.isWithinActiveHours()) {
            LocationWorkScheduler.scheduleNextWork(appContext);
            return Result.success();
        }

        try {
            Location location = fetchCurrentLocation();
            if (location != null) {
                persistLocation(location);
            }

            LocationWorkScheduler.scheduleNextWork(appContext);
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }

    private Location fetchCurrentLocation() throws Exception {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        Task<Location> task;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setDurationMillis(TimeUnit.SECONDS.toMillis(LOCATION_TIMEOUT_SECONDS))
                    .build();
            task = fusedLocationClient.getCurrentLocation(request, tokenSource.getToken());
        } else {
            task = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    tokenSource.getToken());
        }

        return Tasks.await(task, LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void persistLocation(@NonNull Location location) {
        LocationLog log = new LocationLog(
                location.getLatitude(),
                location.getLongitude(),
                location.hasAccuracy() ? location.getAccuracy() : 0f,
                System.currentTimeMillis(),
                location.getProvider() != null ? location.getProvider() : "unknown"
        );
        locationLogDao.insert(log);

        long threshold = System.currentTimeMillis() - DATA_RETENTION_MILLIS;
        locationLogDao.deleteOlderThan(threshold);
    }
}

