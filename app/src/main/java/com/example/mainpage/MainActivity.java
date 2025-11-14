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
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;

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
        previousRecordsButton.setText(Html.fromHtml("<u>과거의 하루를 보러 가기.</u>"));

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

        // 굵게 + 색상 적용
        applyBoldAndColor(spannable, date, fullText);
        applyBoldAndColor(spannable, recordCountStr, fullText);
        applyBoldAndColor(spannable, location, fullText);
        applyBoldAndColor(spannable, recipientName, fullText);

        summaryTextView.setText(spannable);
    }


    //주어진 텍스트 부분에 검은색 볼드 스타일을 적용하는 헬퍼 함수
    private void applyBoldAndColor(SpannableString spannable, String targetText, String fullText) {
        int startIndex = fullText.indexOf(targetText);

        if (startIndex >= 0) {
            int endIndex = startIndex + targetText.length();

            // 굵게
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 색상 적용 (검정색)
            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    //채팅 메시지 추가
    private void addMessage(String sender, String text, int gravity) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(24, 16, 24, 16); // 말풍선 안 여백
        textView.setTextSize(16);

        // GradientDrawable로 말풍선 배경 만들기
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);

        // dp → px 변환
        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = 20 * density; // 일반 모서리
        float tailRadius = 2 * density;    // 꼬리 모서리

        if (sender.equals("챗봇")) {
            bg.setColor(Color.parseColor("#E0E0E0")); // 배경색
            bg.setCornerRadii(new float[]{
                    cornerRadius, cornerRadius,  // top-left
                    cornerRadius, cornerRadius,  // top-right
                    cornerRadius, cornerRadius,  // bottom-right
                    tailRadius, tailRadius       // bottom-left 꼬리
            });
            textView.setTextColor(Color.BLACK); // 글자색
        } else { // 사용자
            bg.setColor(Color.parseColor("#1A73E8"));
            bg.setCornerRadii(new float[]{
                    cornerRadius, cornerRadius,  // top-left
                    cornerRadius, cornerRadius,  // top-right
                    tailRadius, tailRadius,      // bottom-right 꼬리
                    cornerRadius, cornerRadius   // bottom-left
            });
            textView.setTextColor(Color.WHITE);
        }

        textView.setBackground(bg);

        // 레이아웃 파라미터
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.gravity = gravity;
        params.topMargin = 10;

        // 화면 모서리와 여백
        int sideMargin = (int)(16 * density); // 16dp → px
        if (sender.equals("챗봇")) {
            params.leftMargin = sideMargin;
            params.rightMargin = sideMargin / 2;
        } else { // 사용자
            params.leftMargin = sideMargin / 2;
            params.rightMargin = sideMargin;
        }

        textView.setLayoutParams(params);

        // 채팅 레이아웃에 추가
        chatbot.addView(textView);

        // 자동 스크롤
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }



}