package com.example.mainpage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Diary diary);

    @Query("SELECT * FROM diaries ORDER BY createdAt DESC")
    List<Diary> getAll();

    @Query("SELECT * FROM diaries WHERE id = :id")
    Diary getById(long id);

    @Query("SELECT COUNT(*) FROM diaries")
    int getCount();

    @Query("DELETE FROM diaries WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM diaries")
    void deleteAll();
}

