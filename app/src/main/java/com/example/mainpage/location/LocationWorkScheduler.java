package com.example.mainpage.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class LocationWorkScheduler {

    private static final String UNIQUE_WORK_NAME = "location_logging_bootstrap";
    private static final int START_HOUR = 8;
    private static final int END_HOUR = 22;
    private static final int INTERVAL_MINUTES = 10;

    private LocationWorkScheduler() {}

    public static void scheduleInitialWork(@NonNull Context context) {
        long delay = calculateInitialDelayMillis();
        OneTimeWorkRequest request = buildRequest(delay);
        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
        );
    }

    public static void scheduleNextWork(@NonNull Context context) {
        long delay = calculateNextDelayMillis();
        OneTimeWorkRequest request = buildRequest(delay);
        WorkManager.getInstance(context).enqueue(request);
    }

    public static boolean isWithinActiveHours() {
        Calendar now = Calendar.getInstance();
        Calendar start = createBoundary(START_HOUR);
        Calendar end = createBoundary(END_HOUR);
        return !now.before(start) && !now.after(end);
    }

    private static long calculateInitialDelayMillis() {
        Calendar now = Calendar.getInstance();
        Calendar start = createBoundary(START_HOUR);
        Calendar end = createBoundary(END_HOUR);

        if (now.before(start)) {
            return start.getTimeInMillis() - now.getTimeInMillis();
        } else if (now.after(end)) {
            start.add(Calendar.DAY_OF_YEAR, 1);
            return start.getTimeInMillis() - now.getTimeInMillis();
        } else {
            return 0;
        }
    }

    private static long calculateNextDelayMillis() {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.add(Calendar.MINUTE, INTERVAL_MINUTES);

        Calendar end = createBoundary(END_HOUR);
        if (next.after(end)) {
            Calendar start = createBoundary(START_HOUR);
            start.add(Calendar.DAY_OF_YEAR, 1);
            return Math.max(0, start.getTimeInMillis() - now.getTimeInMillis());
        }

        return Math.max(0, next.getTimeInMillis() - now.getTimeInMillis());
    }

    private static OneTimeWorkRequest buildRequest(long delayMillis) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        return new OneTimeWorkRequest.Builder(LocationLoggingWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("location_logging")
                .build();
    }

    private static Calendar createBoundary(int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}

