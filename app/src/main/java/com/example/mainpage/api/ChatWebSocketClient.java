//ChatWebSocketClient.java
package com.example.mainpage.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChatWebSocketClient {
    private static final String TAG = "ChatWebSocketClient";
    private static final String WS_URL = "ws://10.0.2.2:3000/"; // 에뮬레이터용 localhost
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;
    private ChatCallback callback;
    
    public interface ChatCallback {
        void onGenerateStarted();
        void onMessageReceived(String message);
        void onSessionEnded(String message);
        void onSummaryCompleted(String summary); // 요약 완료 이벤트
        void onError(String error);
        void onConnected();
        void onDisconnected();
    }
    
    public ChatWebSocketClient(ChatCallback callback) {
        this.callback = callback;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }
    
    public void connect() {
        Request request = new Request.Builder()
                .url(WS_URL)
                .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onConnected();
                    }
                });
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Message received: " + text);
                handleMessage(text);
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDisconnected();
                    }
                });
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: " + t.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("연결 실패: " + t.getMessage());
                    }
                });
            }
        });
    }
    
    public void sendMessage(String text) {
        if (webSocket == null) {
            if (callback != null) {
                callback.onError("WebSocket이 연결되지 않았습니다.");
            }
            return;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        
        String jsonString = gson.toJson(json);
        Log.d(TAG, "Sending message: " + jsonString);
        webSocket.send(jsonString);
    }
    
    public void requestSummary() {
        if (webSocket == null) {
            if (callback != null) {
                callback.onError("WebSocket이 연결되지 않았습니다.");
            }
            return;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("action", "request_summary");
        
        String jsonString = gson.toJson(json);
        Log.d(TAG, "Requesting summary: " + jsonString);
        webSocket.send(jsonString);
    }
    
    private void handleMessage(String text) {
        try {
            JsonObject json = gson.fromJson(text, JsonObject.class);
            
            // generate_started 상태
            if (json.has("status") && "generate_started".equals(json.get("status").getAsString())) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onGenerateStarted();
                    }
                });
                return;
            }
            
            // 전체 메시지 수신
            if (json.has("fullMessage")) {
                String message = json.get("fullMessage").getAsString();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onMessageReceived(message);
                    }
                });
                return;
            }
            
            // 세션 종료
            if (json.has("status") && "session_ended".equals(json.get("status").getAsString())) {
                String message = json.has("message") ? json.get("message").getAsString() : "세션이 종료되었습니다.";
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSessionEnded(message);
                    }
                });
                return;
            }
            
            // 요약 완료
            if (json.has("status") && "summary_completed".equals(json.get("status").getAsString())) {
                String summary = json.has("summary") ? json.get("summary").getAsString() : "";
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSummaryCompleted(summary);
                    }
                });
                return;
            }
            
            // 요약 완료 (다른 형식: summary 필드가 직접 있는 경우)
            if (json.has("summary")) {
                String summary = json.get("summary").getAsString();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSummaryCompleted(summary);
                    }
                });
                return;
            }
            
            // 에러
            if (json.has("channel") && "error".equals(json.get("channel").getAsString())) {
                String errorMessage = json.has("message") ? json.get("message").getAsString() : "알 수 없는 오류";
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                });
                return;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message: " + e.getMessage());
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onError("메시지 파싱 오류: " + e.getMessage());
                }
            });
        }
    }
    
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
    }
    
    public boolean isConnected() {
        return webSocket != null;
    }
}

