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
    
    /**
     * 특정 날짜의 위치 데이터를 시간순으로 조회합니다.
     * @param startOfDay 해당 날짜의 시작 시간 (밀리초)
     * @param endOfDay 해당 날짜의 끝 시간 (밀리초)
     * @return 해당 날짜의 위치 로그 리스트
     */
    @Query("SELECT * FROM location_logs WHERE recordedAt >= :startOfDay AND recordedAt < :endOfDay ORDER BY recordedAt ASC")
    List<LocationLog> loadByDateRange(long startOfDay, long endOfDay);
    
    @Query("DELETE FROM location_logs")
    void deleteAll();
}

