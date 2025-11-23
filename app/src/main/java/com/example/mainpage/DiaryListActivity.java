//DiaryListActivity.java
package com.example.mainpage;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Paint;
import android.view.View;
import android.content.Intent;

import com.example.mainpage.location.LocationDatabase;



public class DiaryListActivity extends AppCompatActivity {

    private DiaryDao diaryDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        // DB 초기화
        diaryDao = LocationDatabase.getInstance(this).diaryDao();

        // 날짜 표시
        TextView tvDate = findViewById(R.id.tv_date);
        String today = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new Date());
        tvDate.setText(today);

        // DB에서 일기 목록 가져오기 (백그라운드 스레드에서)
        databaseExecutor.execute(() -> {
            List<Diary> diaries = diaryDao.getAll();
            int diaryCount = diaries.size();

            runOnUiThread(() -> {
                // 기록 개수 표시
                TextView tvCount = findViewById(R.id.tv_count);
                String fullText = "벌써 " + diaryCount + "개의 기록";
                tvCount.setText(styleDiaryCount(fullText, diaryCount + "개"));

                // 일기 리스트 동적 추가
                LinearLayout diaryContainer = findViewById(R.id.diaryContainer);
                for (Diary diary : diaries) {
                    addDiary(diaryContainer, diary);
                }
            });
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }

    // 숫자만 Bold + 검은색, 나머지 회색 처리 헬퍼
    private SpannableStringBuilder styleDiaryCount(String fullText, String boldPart) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

        int start = fullText.indexOf(boldPart);
        int end = start + boldPart.length();

        // 숫자 부분: Bold + 검은색
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(0xFF000000), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 나머지 텍스트: 진한 회색
        int gray = 0xFF888888;
        if (start > 0) ssb.setSpan(new ForegroundColorSpan(gray), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (end < fullText.length()) ssb.setSpan(new ForegroundColorSpan(gray), end, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return ssb;
    }

    // 일기 아이템 추가
    private void addDiary(LinearLayout container, Diary diary) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View item = inflater.inflate(R.layout.diary_item, container, false);

        TextView dateText = item.findViewById(R.id.diary_date);
        TextView locationText = item.findViewById(R.id.diary_location);

        dateText.setText(diary.date);
        dateText.setPaintFlags(dateText.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        locationText.setText(diary.location);

        // 클릭 이벤트 추가
        item.setOnClickListener(v -> {
            Intent intent = new Intent(DiaryListActivity.this, DiaryContentActivity.class);
            intent.putExtra("diaryId", diary.id);
            intent.putExtra("date", diary.date);
            intent.putExtra("location", diary.location);
            intent.putExtra("content", diary.content);
            
            // 기록 개수는 백그라운드에서 가져오기
            databaseExecutor.execute(() -> {
                int diaryCount = diaryDao.getCount();
                intent.putExtra("diaryCount", diaryCount);
                runOnUiThread(() -> {
                    startActivity(intent);
                });
            });
        });
        container.addView(item);
    }



}
