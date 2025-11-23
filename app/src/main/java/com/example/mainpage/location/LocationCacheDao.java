package com.example.mainpage.location;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface LocationCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocationCache cache);

    @Query("SELECT districtName FROM location_cache WHERE coordinateKey = :key")
    String getDistrictName(String key);

    @Query("DELETE FROM location_cache WHERE cachedAt < :threshold")
    void deleteOlderThan(long threshold);
}

