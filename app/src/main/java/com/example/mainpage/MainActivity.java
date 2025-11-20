package com.example.mainpage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Intent;

import com.example.mainpage.location.LocationPermissionHelper;
import com.example.mainpage.location.LocationWorkScheduler;


public class MainActivity extends AppCompatActivity {
    LinearLayout chatbot;
    EditText inputMessage;
    Button sendButton;
    ScrollView scrollView;

    TextView summaryTextView;
    TextView previousRecordsButton;
    LinearLayout infoGroup;
    TextView notaLogo;
    LinearLayout fixedChatBubble;
    LinearLayout dateHeaderContainer;
    
    private boolean isChatStarted = false;
    private static final int REQUEST_FOREGROUND_LOCATION = 1001;
    private static final int REQUEST_BACKGROUND_LOCATION = 1002;

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
        infoGroup = findViewById(R.id.infoGroup);
        fixedChatBubble = findViewById(R.id.fixedChatBubble);
        notaLogo = findViewById(R.id.notaLogo);
        dateHeaderContainer = findViewById(R.id.dateHeaderContainer);
        setupSummaryText();

        //이전 기록 관련
        previousRecordsButton = findViewById(R.id.previousRecordsButton);

        //밑줄 추가
        previousRecordsButton.setText(Html.fromHtml("<u>과거의 하루를 보러 가기.</u>"));

        previousRecordsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DiaryListActivity.class);
            startActivity(intent);
        });
        
        // 초기 상태: "안녕!" 텍스트 설정 및 입력 비활성화
        inputMessage.setText("안녕!");
        inputMessage.setFocusable(false);
        inputMessage.setFocusableInTouchMode(false);
        inputMessage.setCursorVisible(false);


        //전송 버튼 클릭 시
        sendButton.setOnClickListener(v -> {
            String message=inputMessage.getText().toString().trim();
            if (!message.isEmpty()){
                // 첫 번째 전송일 때 화면 전환
                if (!isChatStarted) {
                    startChatMode();
                    isChatStarted = true;
                }
                
                addMessage("나",message);
                inputMessage.setText("");

                new Handler().postDelayed(()->{
                    addMessage("챗봇","답장입니다.");
                },1000);
            }
        });

        prepareLocationLogging();
    }
    
    // 채팅 모드로 전환
    private void startChatMode() {
        // 첫 화면 요소들 숨기기
        infoGroup.setVisibility(View.GONE);
        notaLogo.setVisibility(View.GONE);
        previousRecordsButton.setVisibility(View.GONE);
        
        // 날짜 헤더 추가
        addDateHeader();
        
        // 입력란 활성화 및 힌트 변경
        inputMessage.setFocusable(true);
        inputMessage.setFocusableInTouchMode(true);
        inputMessage.setCursorVisible(true);
        inputMessage.setHint("메시지를 입력하세요");
    }
    
    // 날짜 헤더 추가
    private void addDateHeader() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd - E요일의 기록.", Locale.KOREA);
        String dateStr = dateFormat.format(calendar.getTime());
        
        LayoutInflater inflater = LayoutInflater.from(this);
        View headerView = inflater.inflate(R.layout.chat_date_header, dateHeaderContainer, false);
        
        TextView dateText = headerView.findViewById(R.id.date_header_text);
        dateText.setText(dateStr);
        
        dateHeaderContainer.addView(headerView);
        dateHeaderContainer.setVisibility(View.VISIBLE);
    }

    private void setupSummaryText() {
        // 현재 날짜 가져오기, 포맷
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String todayDate = dateFormat.format(calendar.getTime());

        // 임시 변수
        int recordCount = 123;
        String location = "노원구";
        String recipientName = "000";

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

    //주어진 텍스트 부분에 검은색 스타일을 적용하는 헬퍼 함수
    private void applyBoldAndColor(SpannableString spannable, String targetText, String fullText) {
        int startIndex = fullText.indexOf(targetText);

        if (startIndex >= 0) {
            int endIndex = startIndex + targetText.length();

            // 색상 적용 (검정색 #000000)
            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    //채팅 메시지 추가
    private void addMessage(String sender, String text) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View messageView;

        if (sender.equals("챗봇")) {
            messageView = inflater.inflate(R.layout.other_message, chatbot, false);
        } else { // 사용자
            messageView = inflater.inflate(R.layout.my_message, chatbot, false);
        }

        TextView messageText = messageView.findViewById(R.id.message_text);
        messageText.setText(text);

        // 채팅 레이아웃에 추가
        chatbot.addView(messageView);

        // 자동 스크롤
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void prepareLocationLogging() {
        if (!LocationPermissionHelper.hasForegroundPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    LocationPermissionHelper.foregroundPermissions(),
                    REQUEST_FOREGROUND_LOCATION
            );
            return;
        }

        if (LocationPermissionHelper.shouldRequestBackgroundPermission(this)) {
            LocationPermissionHelper.requestBackgroundPermission(this, REQUEST_BACKGROUND_LOCATION);
            return;
        }

        LocationWorkScheduler.scheduleInitialWork(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FOREGROUND_LOCATION) {
            if (LocationPermissionHelper.hasForegroundPermission(this)) {
                if (LocationPermissionHelper.shouldRequestBackgroundPermission(this)) {
                    LocationPermissionHelper.requestBackgroundPermission(this, REQUEST_BACKGROUND_LOCATION);
                } else {
                    LocationWorkScheduler.scheduleInitialWork(this);
                }
            } else {
                Toast.makeText(this, "위치 권한이 없어 위치 기록을 저장할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (LocationPermissionHelper.hasBackgroundPermission(this)) {
                LocationWorkScheduler.scheduleInitialWork(this);
            } else {
                Toast.makeText(this, "백그라운드 위치 권한이 없어 8시~22시 기록이 제한됩니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}