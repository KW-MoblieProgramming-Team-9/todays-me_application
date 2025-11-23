package com.example.mainpage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diaries")
public class Diary {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public final String date;
    
    @NonNull
    public final String location;
    
    @NonNull
    public final String content;
    
    public final long createdAt;

    public Diary(@NonNull String date,
                 @NonNull String location,
                 @NonNull String content,
                 long createdAt) {
        this.date = date;
        this.location = location;
        this.content = content;
        this.createdAt = createdAt;
    }
}

