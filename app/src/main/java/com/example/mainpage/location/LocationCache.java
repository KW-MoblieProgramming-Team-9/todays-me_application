package com.example.mainpage.location;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_cache")
public class LocationCache {

    @PrimaryKey
    @NonNull
    public final String coordinateKey; // "latitude,longitude" 형식

    @NonNull
    public final String districtName;

    public final long cachedAt;

    public LocationCache(@NonNull String coordinateKey, @NonNull String districtName, long cachedAt) {
        this.coordinateKey = coordinateKey;
        this.districtName = districtName;
        this.cachedAt = cachedAt;
    }
}

