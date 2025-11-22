//DiaryListActivity.java
package com.example.mainpage;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.graphics.Paint;
import android.view.View;
import android.content.Intent;



public class DiaryListActivity extends AppCompatActivity {

    private int diaryCount = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        // 뒤로가기 버튼
        ImageButton backBtn = findViewById(R.id.btn_back_main);
        backBtn.setOnClickListener(v -> finish());

        // 날짜 표시
        TextView tvDate = findViewById(R.id.tv_date);
        String today = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(new Date());
        tvDate.setText(today);


        // 기록 개수 표시
        TextView tvCount = findViewById(R.id.tv_count);
        String fullText = "벌써 " + diaryCount + "개의 기록";

        tvCount.setText(styleDiaryCount(fullText, diaryCount + "개"));

        // 일기 리스트 동적 추가
        LinearLayout diaryContainer = findViewById(R.id.diaryContainer);
        addDiary(diaryContainer, "2025년 10월 23일", "노원구", "홍길동, 김규동");
        addDiary(diaryContainer, "2025년 10월 20일", "강남", "박민수");
        addDiary(diaryContainer, "2025년 10월 18일", "서울대입구", "최지원");
        addDiary(diaryContainer, "2025년 10월 17일", "서울대입구", "최지원");
        addDiary(diaryContainer, "2025년 10월 16일", "서울대입구", "최지원");
        addDiary(diaryContainer, "2025년 10월 15일", "서울대입구", "최지원");
        addDiary(diaryContainer, "2025년 10월 14일", "서울대입구", "최지원");
        addDiary(diaryContainer, "2025년 10월 13일", "서울대입구", "최지원");


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
    // 일기 아이템 추가
    private void addDiary(LinearLayout container, String date, String location, String people) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View item = inflater.inflate(R.layout.diary_item, container, false);

        TextView dateText = item.findViewById(R.id.diary_date);
        TextView locationText = item.findViewById(R.id.diary_location);
        TextView peopleText = item.findViewById(R.id.diary_people);

        dateText.setText(date);
        dateText.setPaintFlags(dateText.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        locationText.setText(location);
        peopleText.setText(people);

        // 클릭 이벤트 추가
        item.setOnClickListener(v -> {
            Intent intent = new Intent(DiaryListActivity.this, DiaryContentActivity.class);

            // 실제 content 전달
            String content = location + "에서 " + people + "와(과) 함께";

            intent.putExtra("date", date);
            intent.putExtra("location", location);
            intent.putExtra("people", people);
            intent.putExtra("content", content); // 여기 수정
            intent.putExtra("diaryCount", diaryCount); // 개수 전달

            startActivity(intent);
        });
        container.addView(item);

    }



}
