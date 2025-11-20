package com.example.mainpage.location;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_logs")
public class LocationLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public final double latitude;
    public final double longitude;
    public final float accuracy;
    public final long recordedAt;
    @NonNull
    public final String provider;

    public LocationLog(double latitude,
                       double longitude,
                       float accuracy,
                       long recordedAt,
                       @NonNull String provider) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.recordedAt = recordedAt;
        this.provider = provider;
    }
}

