//MainActivity.java
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Intent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.mainpage.location.LocationPermissionHelper;
import com.example.mainpage.location.LocationWorkScheduler;
import com.example.mainpage.location.LocationDatabase;
import com.example.mainpage.location.LocationLogDao;
import com.example.mainpage.location.LocationCacheDao;
import com.example.mainpage.location.LocationCache;
import com.example.mainpage.api.ChatWebSocketClient;
import com.example.mainpage.util.LocationFormatter;
import com.example.mainpage.Diary;
import com.example.mainpage.DiaryDao;


public class MainActivity extends AppCompatActivity {
    LinearLayout chatbot;
    EditText inputMessage;
    Button sendButton;
    ScrollView scrollView;

    TextView summaryTextView;
    TextView previousRecordsButton;
    LinearLayout infoGroup;
    LinearLayout fixedChatBubble;
    LinearLayout dateHeaderContainer;
    
    private boolean isChatStarted = false;
    private View typingIndicator = null;
    private ChatWebSocketClient webSocketClient;
    private LocationFormatter locationFormatter;
    private boolean isSummaryRequested = false; // 요약 요청 상태
    private static final int REQUEST_FOREGROUND_LOCATION = 1001;
    private static final int REQUEST_BACKGROUND_LOCATION = 1002;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

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

        // WebSocket 클라이언트 초기화
        initializeWebSocket();
        
        // LocationFormatter 초기화
        locationFormatter = new LocationFormatter(this);

        //전송 버튼 클릭 시
        sendButton.setOnClickListener(v -> {
            String message=inputMessage.getText().toString().trim();
            if (!message.isEmpty()){
                // 첫 번째 전송일 때 화면 전환
                if (!isChatStarted) {
                    startChatMode();
                    isChatStarted = true;
                    
                    // 첫 메시지 전송 시 위치 정보를 먼저 전송
                    sendLocationInfoFirst(message);
                    return;
                }
                
                // WebSocket 연결 확인
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    addMessage("나", message);
                    inputMessage.setText("");
                    
                    // 서버로 메시지 전송
                    webSocketClient.sendMessage(message);
                } else {
                    Toast.makeText(this, "서버와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        prepareLocationLogging();
    }
    
    // WebSocket 초기화
    private void initializeWebSocket() {
        webSocketClient = new ChatWebSocketClient(new ChatWebSocketClient.ChatCallback() {
            @Override
            public void onGenerateStarted() {
                // AI가 응답 생성 시작
                showTypingIndicator();
            }

            @Override
            public void onMessageReceived(String message) {
                // AI 응답 수신
                hideTypingIndicator();
                addMessage("챗봇", message);
            }

            @Override
            public void onSessionEnded(String message) {
                // 세션 종료 처리
                hideTypingIndicator();
                
                // 1. 사용자에게 메시지 표시
                addMessage("챗봇", "그러니까 정리하자면");
                
                // 2. 입력중 말풍선 표시
                showTypingIndicator();
                
                // 3. 요약 요청
                isSummaryRequested = true;
                webSocketClient.requestSummary();
            }
            
            @Override
            public void onSummaryCompleted(String summary) {
                // 요약 완료
                hideTypingIndicator();
                
                // 일기 뷰어 페이지로 이동
                navigateToDiaryViewer(summary);
            }

            @Override
            public void onError(String error) {
                // 에러 발생
                hideTypingIndicator();
                Toast.makeText(MainActivity.this, "오류: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnected() {
                // WebSocket 연결됨
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "서버에 연결되었습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                // WebSocket 연결 해제됨
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // 연결 시작
        webSocketClient.connect();
    }
    
    // 채팅 모드로 전환
    private void startChatMode() {
        // 첫 화면 요소들 숨기기
        infoGroup.setVisibility(View.GONE);
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

        // DB에서 기록 개수와 가장 오래 있던 위치 가져오기 (백그라운드 스레드에서)
        DiaryDao diaryDao = LocationDatabase.getInstance(this).diaryDao();
        LocationLogDao locationLogDao = LocationDatabase.getInstance(this).locationLogDao();
        
        databaseExecutor.execute(() -> {
            int recordCount = diaryDao.getCount();
            String location = getLongestStayedLocation(locationLogDao);
            
            runOnUiThread(() -> {
                updateSummaryText(todayDate, recordCount, location);
            });
        });
    }
    
    /**
     * 오늘 가장 오래 있던 위치를 찾아서 행정동 이름으로 반환합니다.
     * 모든 위치에 대해 먼저 API 요청을 수행하고 결과를 캐싱한 후,
     * 캐시된 결과를 사용해서 가장 오래 있던 위치를 찾습니다.
     */
    private String getLongestStayedLocation(LocationLogDao locationLogDao) {
        try {
            // 오늘 날짜의 시작과 끝 시간 계산
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = calendar.getTimeInMillis();
            
            // 오늘 날짜의 위치 데이터 조회
            List<com.example.mainpage.location.LocationLog> todayLogs = 
                locationLogDao.loadByDateRange(startOfDay, endOfDay);
            
            if (todayLogs.isEmpty()) {
                return "";
            }
            
            // 위치별로 그룹화하고 각 위치에서 머문 시간 계산
            // 간단한 방법: 30분 이내의 위치는 같은 그룹으로 처리
            final long GROUP_INTERVAL_MS = 30 * 60 * 1000; // 30분
            final double LOCATION_THRESHOLD = 0.001; // 약 100m
            
            List<LocationGroup> groups = new ArrayList<>();
            
            com.example.mainpage.location.LocationLog firstLog = todayLogs.get(0);
            long groupStartTime = firstLog.recordedAt;
            double groupLatitude = firstLog.latitude;
            double groupLongitude = firstLog.longitude;
            
            for (int i = 1; i < todayLogs.size(); i++) {
                com.example.mainpage.location.LocationLog currentLog = todayLogs.get(i);
                com.example.mainpage.location.LocationLog previousLog = todayLogs.get(i - 1);
                
                // 위치가 크게 바뀌었거나 시간 간격이 30분을 초과하면 새로운 그룹 시작
                double latDiff = Math.abs(currentLog.latitude - groupLatitude);
                double lonDiff = Math.abs(currentLog.longitude - groupLongitude);
                boolean locationChanged = latDiff > LOCATION_THRESHOLD || lonDiff > LOCATION_THRESHOLD;
                boolean timeGap = currentLog.recordedAt - previousLog.recordedAt > GROUP_INTERVAL_MS;
                
                if (locationChanged || timeGap) {
                    // 이전 그룹 저장
                    groups.add(new LocationGroup(
                            groupStartTime,
                            previousLog.recordedAt,
                            groupLatitude,
                            groupLongitude
                    ));
                    
                    // 새 그룹 시작
                    groupStartTime = currentLog.recordedAt;
                    groupLatitude = currentLog.latitude;
                    groupLongitude = currentLog.longitude;
                }
            }
            
            // 마지막 그룹 추가
            com.example.mainpage.location.LocationLog lastLog = todayLogs.get(todayLogs.size() - 1);
            groups.add(new LocationGroup(
                    groupStartTime,
                    lastLog.recordedAt,
                    groupLatitude,
                    groupLongitude
            ));
            
            // DB 캐시 DAO 가져오기
            LocationCacheDao cacheDao = LocationDatabase.getInstance(this).locationCacheDao();
            
            // 오래된 캐시 정리 (다음날 삭제 - 오늘 자정 이전의 캐시 삭제)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long cacheRetentionThreshold = today.getTimeInMillis();
            cacheDao.deleteOlderThan(cacheRetentionThreshold);
            
            // 모든 위치 그룹에 대해 API 요청을 수행하고 결과를 캐싱
            Map<String, String> locationCache = new HashMap<>();
            long currentTime = System.currentTimeMillis();
            
            if (locationFormatter != null) {
                for (LocationGroup group : groups) {
                    // 좌표를 키로 사용 (소수점 6자리까지 반올림하여 유사한 좌표를 같은 키로 처리)
                    String key = String.format(Locale.US, "%.6f,%.6f", group.latitude, group.longitude);
                    
                    // 먼저 DB 캐시에서 확인
                    String districtName = cacheDao.getDistrictName(key);
                    
                    // DB 캐시에 없으면 API 요청
                    if (districtName == null || districtName.isEmpty()) {
                        districtName = locationFormatter.getDistrictName(group.longitude, group.latitude);
                        String finalDistrictName = districtName != null ? districtName : "노원구 월계동";
                        
                        // DB 캐시에 저장
                        LocationCache cache = new LocationCache(key, finalDistrictName, currentTime);
                        cacheDao.insert(cache);
                        
                        locationCache.put(key, finalDistrictName);
                    } else {
                        // DB 캐시에 있으면 메모리 캐시에도 추가
                        locationCache.put(key, districtName);
                    }
                }
            }
            
            // 가장 오래 머문 위치 찾기
            LocationGroup longestGroup = null;
            long maxDuration = 0;
            
            for (LocationGroup group : groups) {
                long duration = group.endTime - group.startTime;
                if (duration > maxDuration) {
                    maxDuration = duration;
                    longestGroup = group;
                }
            }
            
            if (longestGroup == null) {
                return "";
            }
            
            // 캐시된 결과에서 가장 오래 있던 위치의 행정동 이름 반환
            String key = String.format(Locale.US, "%.6f,%.6f", longestGroup.latitude, longestGroup.longitude);
            String districtName = locationCache.get(key);
            return districtName != null && !districtName.isEmpty() ? districtName : "노원구 월계동";
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "가장 오래 있던 위치 찾기 오류: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 위치 그룹을 나타내는 내부 클래스
     */
    private static class LocationGroup {
        final long startTime;
        final long endTime;
        final double latitude;
        final double longitude;
        
        LocationGroup(long startTime, long endTime, double latitude, double longitude) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private void updateSummaryText(String date, int recordCount, String location) {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String recordCountStr = String.valueOf(recordCount);
        String fullText;
        SpannableString spannable;

        // 22시 이전이면 간단한 메시지만 표시
        String recordPrefix = "벌써 ";
        if (recordCountStr.equals("0")) {
            recordPrefix = "아직 ";
        }
        if (currentHour < 22) {
            fullText = date + ".\n" +
                    recordPrefix + recordCountStr + "개의 기록.\n" +
                    "조금 후에 다시 찾아와주세요.";
            
            spannable = new SpannableString(fullText);
            
            // 굵게 + 색상 적용
            applyBoldAndColor(spannable, date, fullText);
            applyBoldAndColor(spannable, recordCountStr, fullText);
        } else {
            // 22시 이후면 기존 텍스트 표시
            fullText = date + ".\n" +
                    recordPrefix + recordCountStr + "개의 기록.\n" +
                    "오늘은 " + location + "에 오래 계셨어요.\n" +
                    "당신의 하루가 궁금해요.\n" +
                    "'오늘의 나'와 당신의 하루를 나눠봐요.";

            spannable = new SpannableString(fullText);

            // 굵게 + 색상 적용
            applyBoldAndColor(spannable, date, fullText);
            applyBoldAndColor(spannable, recordCountStr, fullText);
            applyBoldAndColor(spannable, location, fullText);
        }

        summaryTextView.setText(spannable);
    }

    //주어진 텍스트 부분에 검은색 스타일과 bold를 적용하는 헬퍼 함수
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
            
            // Bold 스타일 적용
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
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
    
    // 입력중 표시 추가
    private void showTypingIndicator() {
        if (typingIndicator != null) {
            return; // 이미 표시 중이면 리턴
        }
        
        LayoutInflater inflater = LayoutInflater.from(this);
        typingIndicator = inflater.inflate(R.layout.typing_message, chatbot, false);
        
        // 애니메이션 적용
        View dot1 = typingIndicator.findViewById(R.id.typing_dot1);
        View dot2 = typingIndicator.findViewById(R.id.typing_dot2);
        View dot3 = typingIndicator.findViewById(R.id.typing_dot3);
        
        Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_animation);
        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_animation);
        Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_animation);
        
        // 각 점마다 시작 시간 지연
        animation2.setStartOffset(200);
        animation3.setStartOffset(400);
        
        dot1.startAnimation(animation1);
        dot2.startAnimation(animation2);
        dot3.startAnimation(animation3);
        
        chatbot.addView(typingIndicator);
        
        // 자동 스크롤
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
    
    // 입력중 표시 제거
    private void hideTypingIndicator() {
        if (typingIndicator != null) {
            chatbot.removeView(typingIndicator);
            typingIndicator = null;
        }
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
    
    /**
     * 첫 메시지 전송 시 위치 정보를 포함한 시스템 메시지를 먼저 전송합니다.
     */
    private void sendLocationInfoFirst(String userMessage) {
        if (webSocketClient == null || !webSocketClient.isConnected()) {
            Toast.makeText(this, "서버와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("MainActivity", "첫 메시지 전송 시작 - 위치 정보 포함");
        
        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String currentDate = dateFormat.format(calendar.getTime());
        
        // 첫 메시지에 날짜 추가
        String messageWithDate = currentDate + " " + userMessage;
        
        // 위치 정보 포맷팅
        locationFormatter.formatTodayLocations(new LocationFormatter.LocationFormatCallback() {
            @Override
            public void onFormatted(String formattedMessage) {
                android.util.Log.d("MainActivity", "위치 정보 포맷팅 완료. 메시지 길이: " + 
                        (formattedMessage != null ? formattedMessage.length() : 0));
                runOnUiThread(() -> {
                    // 위치 정보가 있으면 먼저 전송
                    if (formattedMessage != null && !formattedMessage.isEmpty()) {
                        android.util.Log.d("MainActivity", "위치 정보 메시지 전송");
                        webSocketClient.sendMessage(formattedMessage);
                    } else {
                        android.util.Log.w("MainActivity", "위치 정보 메시지가 비어있습니다.");
                    }
                    
                    // 사용자 메시지 전송 (날짜 포함)
                    android.util.Log.d("MainActivity", "사용자 메시지 전송: " + messageWithDate);
                    addMessage("나", userMessage);
                    inputMessage.setText("");
                    webSocketClient.sendMessage(messageWithDate);
                });
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e("MainActivity", "위치 정보 포맷팅 오류: " + error);
                runOnUiThread(() -> {
                    // 에러가 발생해도 사용자 메시지는 전송 (날짜 포함)
                    Toast.makeText(MainActivity.this, "위치 정보를 가져오지 못했습니다: " + error, Toast.LENGTH_SHORT).show();
                    addMessage("나", userMessage);
                    inputMessage.setText("");
                    webSocketClient.sendMessage(messageWithDate);
                });
            }
        });
    }
    
    // 일기 뷰어 페이지로 이동
    private void navigateToDiaryViewer(String summary) {
        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String date = dateFormat.format(calendar.getTime());
        
        // 요약 내용
        String content = summary != null ? summary : "";

        // DB에 일기 저장 (백그라운드 스레드에서)
        DiaryDao diaryDao = LocationDatabase.getInstance(this).diaryDao();
        LocationLogDao locationLogDao = LocationDatabase.getInstance(this).locationLogDao();
        
        databaseExecutor.execute(() -> {
            // 가장 오래 있던 위치 가져오기
            String location = getLongestStayedLocation(locationLogDao);
            
            // DB에 일기 저장
            Diary diary = new Diary(date, location, content, System.currentTimeMillis());
            long diaryId = diaryDao.insert(diary);
            int diaryCount = diaryDao.getCount();
            
            runOnUiThread(() -> {
                // Intent 생성 및 데이터 전달
                Intent intent = new Intent(MainActivity.this, DiaryContentActivity.class);
                intent.putExtra("diaryId", diaryId);
                intent.putExtra("date", date);
                intent.putExtra("location", location);
                intent.putExtra("content", content);
                intent.putExtra("diaryCount", diaryCount);
                
                startActivity(intent);
            });
        });
        
        // 현재 채팅 화면은 종료하지 않고 백스택에 남김
        // finish(); // 필요시 주석 해제
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // WebSocket 연결 해제
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        // LocationFormatter 종료
        if (locationFormatter != null) {
            locationFormatter.shutdown();
        }
        // ExecutorService 종료
        databaseExecutor.shutdown();
    }
}