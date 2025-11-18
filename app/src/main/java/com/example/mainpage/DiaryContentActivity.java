package com.example.mainpage;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiaryContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_content);

        ImageButton backBtn = findViewById(R.id.btn_back_main);
        backBtn.setOnClickListener(v -> finish());

        // 날짜, 내용 등 받기
        String date = getIntent().getStringExtra("date");
        String location = getIntent().getStringExtra("location");
        String people = getIntent().getStringExtra("people");
        String content = getIntent().getStringExtra("content");
        int diaryCount = getIntent().getIntExtra("diaryCount", 0); // 기본값 0

        // TextView 세팅
        TextView tvDate = findViewById(R.id.tv_date);
        tvDate.setText(date);

        TextView tvCount = findViewById(R.id.tv_count);


        TextView tvContent = findViewById(R.id.tv_content);
//
//        if(content == null || content.isEmpty()) {
//            content = location + "에서 " + people + "와(과) 함께"; // 기본 내용
//        }
        tvContent.setText(content);

    }
}
