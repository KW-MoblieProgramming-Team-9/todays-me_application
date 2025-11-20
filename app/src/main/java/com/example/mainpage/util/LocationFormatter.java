package com.example.mainpage.util;

import android.content.Context;
import android.util.Log;

import com.example.mainpage.api.DistrictNameApiClient;
import com.example.mainpage.location.LocationDatabase;
import com.example.mainpage.location.LocationLog;
import com.example.mainpage.location.LocationLogDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationFormatter {
    private static final String TAG = "LocationFormatter";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH시 mm분", Locale.KOREA);
    
    private final Context context;
    private final DistrictNameApiClient apiClient;
    private final LocationLogDao locationLogDao;
    private final ExecutorService executorService;
    
    public interface LocationFormatCallback {
        void onFormatted(String formattedMessage);
        void onError(String error);
    }
    
    public LocationFormatter(Context context) {
        this.context = context;
        this.apiClient = new DistrictNameApiClient();
        this.locationLogDao = LocationDatabase.getInstance(context).locationLogDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 오늘 날짜의 위치 데이터를 시간대별로 그룹화하고 행정동으로 변환하여 포맷팅합니다.
     * 비동기로 실행되며 결과는 콜백으로 전달됩니다.
     */
    public void formatTodayLocations(LocationFormatCallback callback) {
        executorService.execute(() -> {
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
                List<LocationLog> todayLogs = locationLogDao.loadByDateRange(startOfDay, endOfDay);
                
                if (todayLogs.isEmpty()) {
                    Log.d(TAG, "오늘 날짜의 위치 데이터가 없습니다.");
                    // 위치 데이터가 없어도 기본 시스템 메시지 전송
                    StringBuilder emptyMessageBuilder = new StringBuilder();
                    emptyMessageBuilder.append("다음은 사용자가 오늘 방문한 장소 리스트입니다.\n\n");
                    emptyMessageBuilder.append("(오늘 방문한 장소가 없습니다)\n\n");
                    emptyMessageBuilder.append("위 방문 장소를 토대로 사용자와 친근하게 대화하여 사용자의 일기 작성을 도우십시오.\n\n");
                    emptyMessageBuilder.append("대답은 짧게 합니다.\n\n");
                    emptyMessageBuilder.append("이제부터 당신은 사용자와 대화합니다.\n\n");
                    emptyMessageBuilder.append("---\n\n");
                    callback.onFormatted(emptyMessageBuilder.toString());
                    return;
                }
                
                Log.d(TAG, "오늘 날짜의 위치 데이터 개수: " + todayLogs.size());
                
                // 위치 데이터를 시간대별로 그룹화
                List<TimeLocationGroup> groups = groupByTimeRange(todayLogs);
                Log.d(TAG, "그룹화된 위치 데이터 개수: " + groups.size());
                
                // 각 그룹의 위치를 행정동으로 변환
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("다음은 사용자가 오늘 방문한 장소 리스트입니다.\n\n");
                
                for (TimeLocationGroup group : groups) {
                    String districtName = apiClient.getDistrictName(
                            group.longitude, 
                            group.latitude
                    );
                    
                    if (districtName == null || districtName.isEmpty()) {
                        districtName = "위치 정보 없음";
                        Log.w(TAG, "행정동 정보를 가져오지 못했습니다. 좌표: " + group.longitude + ", " + group.latitude);
                    } else {
                        Log.d(TAG, "행정동 정보: " + districtName);
                    }
                    
                    String startTime = TIME_FORMAT.format(group.startTime);
                    String endTime = TIME_FORMAT.format(group.endTime);
                    
                    messageBuilder.append(startTime)
                                  .append("~ ")
                                  .append(endTime)
                                  .append(": ")
                                  .append(districtName)
                                  .append("\n");
                }
                
                messageBuilder.append("\n위 방문 장소를 토대로 사용자와 친근하게 대화하여 사용자의 일기 작성을 도우십시오.\n\n");
                messageBuilder.append("대답은 짧게 합니다.\n\n");
                messageBuilder.append("이제부터 당신은 사용자와 대화합니다.\n\n");
                messageBuilder.append("---\n\n");
                
                String formattedMessage = messageBuilder.toString();
                Log.d(TAG, "포맷팅된 메시지 길이: " + formattedMessage.length());
                callback.onFormatted(formattedMessage);
                
            } catch (Exception e) {
                Log.e(TAG, "위치 포맷팅 중 오류 발생: " + e.getMessage());
                callback.onError("위치 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }
    
    /**
     * 위치 로그를 시간대별로 그룹화합니다.
     * 시간 간격이 30분 이내이면 같은 그룹으로 처리합니다.
     * 각 그룹의 대표 위치는 그룹의 첫 번째 위치를 사용합니다.
     */
    private List<TimeLocationGroup> groupByTimeRange(List<LocationLog> logs) {
        List<TimeLocationGroup> groups = new ArrayList<>();
        
        if (logs.isEmpty()) {
            return groups;
        }
        
        // 간단한 그룹화: 시간 간격이 30분 이내이면 같은 그룹으로 처리
        final long GROUP_INTERVAL_MS = 30 * 60 * 1000; // 30분
        
        LocationLog firstLog = logs.get(0);
        long groupStartTime = firstLog.recordedAt;
        double groupLatitude = firstLog.latitude;
        double groupLongitude = firstLog.longitude;
        
        for (int i = 1; i < logs.size(); i++) {
            LocationLog currentLog = logs.get(i);
            LocationLog previousLog = logs.get(i - 1);
            
            // 시간 간격이 30분을 초과하면 새로운 그룹 시작
            if (currentLog.recordedAt - previousLog.recordedAt > GROUP_INTERVAL_MS) {
                // 이전 그룹 저장 (그룹의 첫 번째 위치 사용)
                groups.add(new TimeLocationGroup(
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
        LocationLog lastLog = logs.get(logs.size() - 1);
        groups.add(new TimeLocationGroup(
                groupStartTime,
                lastLog.recordedAt,
                groupLatitude,
                groupLongitude
        ));
        
        return groups;
    }
    
    /**
     * 시간대별 위치 그룹을 나타내는 내부 클래스
     */
    private static class TimeLocationGroup {
        final long startTime;
        final long endTime;
        final double latitude;
        final double longitude;
        
        TimeLocationGroup(long startTime, long endTime, double latitude, double longitude) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}

