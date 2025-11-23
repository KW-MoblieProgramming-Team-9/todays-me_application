package com.example.mainpage.location;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mainpage.Diary;
import com.example.mainpage.DiaryDao;

@Database(entities = {LocationLog.class, Diary.class, LocationCache.class}, version = 4, exportSchema = false)
public abstract class LocationDatabase extends RoomDatabase {

    private static final String DB_NAME = "location_logs.db";
    private static volatile LocationDatabase INSTANCE;

    public abstract LocationLogDao locationLogDao();
    
    public abstract DiaryDao diaryDao();
    
    public abstract LocationCacheDao locationCacheDao();

    public static LocationDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    LocationDatabase.class,
                                    DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

