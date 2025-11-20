package com.example.mainpage.location;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(LocationLog log);

    @Query("SELECT * FROM location_logs ORDER BY recordedAt DESC LIMIT :limit")
    List<LocationLog> loadLatest(int limit);

    @Query("DELETE FROM location_logs WHERE recordedAt < :threshold")
    void deleteOlderThan(long threshold);
}

