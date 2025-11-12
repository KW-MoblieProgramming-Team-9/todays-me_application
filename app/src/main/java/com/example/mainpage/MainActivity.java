package com.example.mainpage;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    LinearLayout chatbot;
    EditText inputMessage;
    Button sendButton;
    ScrollView scrollView;

    TextView summaryTextView;
    TextView previousRecordsButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //채팅 관련 뷰
        chatbot = findViewById(R.id.chatbot);
        inputMessage= findViewById(R.id.inputMessage);
        sendButton=findViewById(R.id.sendButton);
        scrollView=findViewById(R.id.scrollview);

        //상단 텍스트뷰 관련
        summaryTextView = findViewById(R.id.summaryTextView);
        setupSummaryText();

        //이전 기록 관련
        previousRecordsButton = findViewById(R.id.previousRecordsButton);

        //밑줄 추가
        previousRecordsButton.setText(Html.fromHtml("<u>과거의 하루를 보러 가기</u>"));

        //전송 버튼 클릭 시
        sendButton.setOnClickListener(v -> {
            String message=inputMessage.getText().toString().trim();
            if (!message.isEmpty()){
                addMessage("나",message, Gravity.END);
                inputMessage.setText("");

                new Handler().postDelayed(()->{
                    addMessage("챗봇",message,Gravity.START);
                },1000);
            }
        });


    }
    private void setupSummaryText() {
        // 현재 날짜 가져오기, 포맷
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String todayDate = dateFormat.format(calendar.getTime());

        // 임시 변수
        int recordCount = 123;
        String location = "노원구";
        String recipientName = "챗봇";

        updateSummaryText(todayDate, recordCount, location, recipientName);
    }

    private void updateSummaryText(String date, int recordCount, String location, String recipientName) {

        String recordCountStr = String.valueOf(recordCount);

        // 전체 텍스트 구성
        String fullText = date + ".\n" +
                "벌써 " + recordCountStr + "개의 기록.\n" +
                "오늘은 " + location + "에 오래 계셨어요.\n" +
                "당신의 하루가 궁금해요.\n" +
                recipientName + "에게 당신의 하루를 들려주세요.";

        SpannableString spannable = new SpannableString(fullText);

        // 볼드 적용할 변수들
        applyBoldStyle(spannable, date, fullText);
        applyBoldStyle(spannable, recordCountStr, fullText);
        applyBoldStyle(spannable, location, fullText);
        applyBoldStyle(spannable, recipientName, fullText);

        summaryTextView.setText(spannable);
    }

    //주어진 텍스트 부분에 굵게(Bold) 스타일을 적용하는 헬퍼 함수
    private void applyBoldStyle(
            SpannableString spannable,
            String targetText,
            String fullText) {

        int startIndex = fullText.indexOf(targetText);

        if (startIndex >= 0) {
            int endIndex = startIndex + targetText.length();

            // 굵게 스타일 (Typeface.BOLD) 적용
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    //채팅 메시지 추가
    private void addMessage(String sender, String text, int gravity) {
        TextView textView=new TextView(this);
        textView.setText(sender + ": " + text);
        textView.setPadding(16, 8, 16, 8);
        textView.setTextSize(16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = gravity;
        params.topMargin = 10;

        textView.setLayoutParams(params);
        textView.setBackgroundResource(
                gravity == Gravity.END ? android.R.color.holo_blue_light : android.R.color.darker_gray);

        chatbot.addView(textView);

        //자동 스크롤 맨 아래로
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}