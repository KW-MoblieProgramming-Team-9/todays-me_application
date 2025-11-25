# Today's Me (MainPage)

하루를 대화로 정리하고, 위치 기록을 바탕으로 일기를 생성/조회하는 Android 앱입니다.

## 프로젝트 개요

- 플랫폼: Android (Java)
- 패키지명: `com.example.mainpage`
- 핵심 기능
  - 챗봇과 대화
  - 대화 종료 후 하루 요약 생성
  - 요약 결과를 일기로 저장
  - 과거 일기 목록/상세 조회
  - 위치 기록 기반 "오늘 오래 머문 동네" 표시

## 기술 스택

- Android SDK (minSdk 24, targetSdk 34)
- Java 8
- Room (로컬 DB)
- WorkManager (주기적 위치 기록 스케줄링)
- OkHttp WebSocket (챗봇 서버 통신)
- Gson (JSON 파싱)

## 실행 전 준비

### 1) Android Studio에서 프로젝트 열기

- 루트 프로젝트를 Android Studio로 엽니다.
- Gradle Sync가 완료되는지 확인합니다.

### 2) 백엔드 서버 실행

앱은 로컬 서버와 통신합니다.

- WebSocket: `ws://10.0.2.2:3000/`
- 지역명 조회 API: `http://10.0.2.2:3000/api/district-name`

`10.0.2.2`는 Android Emulator에서 PC localhost를 가리키는 주소입니다.

### 3) 에뮬레이터/기기 실행

- 에뮬레이터 사용을 권장합니다(현재 주소 설정 기준).
- 실제 기기 사용 시 서버 주소를 PC IP로 바꿔야 동작합니다.

## 앱 사용 과정

### 1. 앱 실행

- 첫 화면(`MainActivity`)에서 오늘 날짜, 기록 수, 안내 문구를 확인합니다.
- `과거의 하루를 보러 가기.`를 누르면 일기 목록 화면으로 이동합니다.

### 2. 권한 허용

앱 시작 후 위치 권한 요청이 뜨면 허용합니다.

- 전경 위치: 현재 위치 수집
- 백그라운드 위치(Android 10+): 8시~22시 기록 정확도 향상

권한을 거부하면 일부 기능(위치 기반 요약)이 제한됩니다.

### 3. 챗봇 대화 시작

- 입력창 기본 텍스트(`안녕!`) 상태에서 전송하면 대화 모드로 전환됩니다.
- 첫 메시지 전송 시
  1) 날짜 정보 포함
  2) 오늘 위치 요약(가능한 경우) 먼저 전송
  3) 사용자 메시지 전송

### 4. 대화 종료 및 요약 생성

- 서버가 세션 종료 상태를 보내면 앱이 요약을 요청합니다.
- 요약 완료 시 자동으로 일기 상세 화면으로 이동합니다.

### 5. 일기 저장

- 생성된 요약은 Room DB(`diaries` 테이블)에 저장됩니다.
- 저장 항목
  - 날짜
  - 위치(가장 오래 머문 지역)
  - 내용(요약 텍스트)
  - 생성 시각

### 6. 과거 일기 조회

- 목록 화면(`DiaryListActivity`)에서 저장된 일기를 확인합니다.
- 항목 클릭 시 상세 화면(`DiaryContentActivity`)으로 이동합니다.

## 위치 기록 동작 방식

- WorkManager를 사용해 08:00~22:00 사이 약 10분 간격으로 위치 기록 작업을 예약합니다.
- 좌표를 행정동 이름으로 변환해 요약 문구에 활용합니다.
- 동일/유사 좌표는 캐시를 사용해 API 호출을 줄입니다.

## 트러블슈팅

### 서버 연결 실패

- 증상: 서버 연결 오류 메시지 또는 WebSocket 오류
- 확인 항목
  - 백엔드가 3000 포트로 실행 중인지
  - 에뮬레이터 사용 중인지(`10.0.2.2` 필요)

### 위치 기반 문구가 비어 있음

- 위치 권한(특히 백그라운드) 허용 여부 확인
- GPS/위치 서비스 활성화 확인

### 실제 기기에서 통신 불가

- `10.0.2.2` 대신 개발 PC의 로컬 네트워크 IP로 변경 필요
- PC 방화벽/포트 허용 확인

## 프로젝트 구조 (핵심)

- `app/src/main/java/com/example/mainpage/MainActivity.java`
- `app/src/main/java/com/example/mainpage/DiaryListActivity.java`
- `app/src/main/java/com/example/mainpage/DiaryContentActivity.java`
- `app/src/main/java/com/example/mainpage/location/*`
- `app/src/main/java/com/example/mainpage/api/*`

## 참고

본 앱은 로컬 백엔드와의 연동을 전제로 동작합니다.
백엔드 미실행 상태에서는 챗봇 응답 및 위치명 변환 기능이 정상 동작하지 않을 수 있습니다.
