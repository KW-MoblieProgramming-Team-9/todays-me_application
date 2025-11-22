//DistrictNameApiClient.java
package com.example.mainpage.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DistrictNameApiClient {
    private static final String TAG = "DistrictNameApiClient";
    private static final String BASE_URL = "http://10.0.2.2:3000"; // 에뮬레이터용 localhost
    private static final String API_ENDPOINT = "/api/district-name";
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public DistrictNameApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * 좌표를 기반으로 행정동 정보를 조회합니다.
     * 
     * @param longitude 경도 (X 좌표)
     * @param latitude 위도 (Y 좌표)
     * @return 행정동 이름 (예: "서울 강남구 대치동"), 실패 시 null
     */
    public String getDistrictName(double longitude, double latitude) {
        String url = BASE_URL + API_ENDPOINT + 
                     "?lot=" + longitude + 
                     "&lat=" + latitude;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "API 호출 실패: " + response.code());
                return null;
            }
            
            String responseBody = response.body() != null ? response.body().string() : null;
            if (responseBody == null) {
                Log.e(TAG, "응답 본문이 null입니다.");
                return null;
            }
            
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // documents 배열 확인
            if (!jsonResponse.has("documents")) {
                Log.e(TAG, "documents 필드가 없습니다.");
                return null;
            }
            
            JsonArray documents = jsonResponse.getAsJsonArray("documents");
            if (documents.size() == 0) {
                Log.e(TAG, "documents 배열이 비어있습니다.");
                return null;
            }
            
            // 첫 번째 문서에서 행정동 정보 추출
            JsonObject document = documents.get(0).getAsJsonObject();
            
            // region_type이 "H"인지 확인 (행정동)
            if (document.has("region_type") && 
                "H".equals(document.get("region_type").getAsString())) {
                
                // address_name 반환
                if (document.has("address_name")) {
                    return document.get("address_name").getAsString();
                }
            }
            
            Log.e(TAG, "행정동 정보를 찾을 수 없습니다.");
            return null;
            
        } catch (IOException e) {
            Log.e(TAG, "API 호출 중 오류 발생: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "응답 파싱 중 오류 발생: " + e.getMessage());
            return null;
        }
    }
}

