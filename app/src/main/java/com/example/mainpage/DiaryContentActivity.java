//DiaryContentActivity.java
package com.example.mainpage;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.mainpage.location.LocationDatabase;
import com.example.mainpage.DiaryDao;

public class DiaryContentActivity extends AppCompatActivity {

    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_content);

        TextView backBtn = findViewById(R.id.btn_back_main);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(DiaryContentActivity.this, DiaryListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Intent에서 받은 데이터 먼저 사용
        String date = getIntent().getStringExtra("date");
        String location = getIntent().getStringExtra("location");
        String content = getIntent().getStringExtra("content");
        int diaryCount = getIntent().getIntExtra("diaryCount", 0);
        
        // TextView 세팅 (Intent 데이터로 먼저 표시)
        TextView tvDate = findViewById(R.id.tv_date);
        if (date != null) {
            tvDate.setText(date);
        }

        TextView tvLocation = findViewById(R.id.tv_location);
        if (location != null && !location.isEmpty()) {
            tvLocation.setText(location + ".");
        } else {
            tvLocation.setVisibility(android.view.View.GONE);
        }

        TextView tvContent = findViewById(R.id.tv_content);
        if (content != null) {
            tvContent.setText(content);
        }

        // DB에서 일기 가져오기 (백그라운드 스레드에서, Intent 데이터가 없을 경우)
        long diaryId = getIntent().getLongExtra("diaryId", -1);
        if (diaryId != -1 && (date == null || content == null)) {
            DiaryDao diaryDao = LocationDatabase.getInstance(this).diaryDao();
            databaseExecutor.execute(() -> {
                Diary diary = diaryDao.getById(diaryId);
                if (diary != null) {
                    runOnUiThread(() -> {
                        if (date == null) tvDate.setText(diary.date);
                        if (location == null) {
                            if (diary.location != null && !diary.location.isEmpty()) {
                                tvLocation.setText(diary.location + ".");
                                tvLocation.setVisibility(android.view.View.VISIBLE);
                            } else {
                                tvLocation.setVisibility(android.view.View.GONE);
                            }
                        }
                        if (content == null) tvContent.setText(diary.content);
                    });
                }
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}
