# TrailGO

MTB 라이더를 위한 Android GPS 라이딩 기록 앱과 Spring 기반 운영 서버입니다.

TrailGO의 현재 MVP는 **게스트 로컬 주행**에 집중합니다. 계정을 만들지 않아도 GPS로 주행을 기록하고, GPX 코스를 따라가며, 기록과 GPX 파일을 기기 안에 안전하게 보관할 수 있습니다.

> Google Play 출시 버전은 게스트 기능을 우선 제공합니다. 회원 동기화와 커뮤니티 기능은 안전한 인증·권한 모델을 완성한 뒤 공개합니다.

## 핵심 기능

- 게스트로 즉시 시작
- GPS 기반 라이딩 시작·일시정지·재개·종료
- 거리, 시간, 현재·평균·최고 속도, 고도 기록
- 화면 잠금과 앱 전환 중에도 포그라운드 서비스로 주행 기록 유지
- 주행 종료 후 GPX 생성 및 기기 내부 저장
- 로컬 기록 목록·상세 조회와 삭제
- 외부 GPX 가져오기와 GPX 코스 따라가기
- 코스 이탈·복귀 상태 및 음성 안내
- 승인된 위험지역의 지도·음성 안내
- SOS: 사용자 확인 후 전화 또는 문자 앱 열기

## 프로젝트 구조

```text
.
├─ app2/                         # Android 앱 (Java / Gradle)
│  └─ app/
│     └─ src/main/java/com/bike/trailgo/
│        ├─ data/                # 게스트 로컬 기록 저장소
│        ├─ ui/riding/           # 주행 화면·Foreground Service·GPX
│        ├─ ui/courses/          # GPX 코스 가져오기·따라가기
│        └─ setting/             # 게스트 설정·개인정보 안내
└─ web/bikeapp/                  # Spring Boot 서버 (Java / Gradle)
   └─ src/main/
      ├─ java/com/bike/webapp/   # 공개 API·관리자 위험지역 검수
      └─ resources/
         ├─ static/              # 소개·지원·정책 페이지
         └─ mapper/              # MyBatis 매퍼
```

## 게스트 기록 정책

게스트 모드의 기록은 서버에 자동 업로드하지 않습니다.

- 주행 중 위치와 미완료 주행 체크포인트는 기기에 저장합니다.
- 완료한 기록은 주행 요약과 GPX를 함께 앱 내부 저장소에 저장합니다.
- 게스트 기록은 항상 비공개입니다.
- 네트워크가 없어도 주행·저장·조회가 가능해야 합니다.
- 향후 회원 동기화는 사용자가 명시적으로 선택했을 때만 실행합니다.

## 시작하기

### 사전 요구사항

- JDK 17 이상 권장
- Android Studio 및 Android SDK
- Android 기기 또는 에뮬레이터
- MariaDB를 사용하는 서버 실행 환경

### Android 앱 실행

Android 설정은 `app2/gradle.properties` 또는 CI의 Gradle 속성으로 제공합니다. 실제 키와 주소는 저장소에 커밋하지 마세요.

필수 속성:

```properties
TRAILGO_SERVER_BASE_URL=https://your-server.example/
```

배포 환경에 따라 추가할 수 있는 속성:

```properties
TRAILGO_SERVER_BASE_URL_DEBUG=https://debug-server.example/
TRAILGO_SERVER_BASE_URL_RELEASE=https://release-server.example/
GYUVIS_BASE_URL=https://assistant-server.example/
KAKAO_APP_KEY=your-native-app-key
GOOGLE_MAPS_API_KEY=your-google-maps-key
TRAILGO_PRIVACY_POLICY_URL=https://your-domain.example/privacy
TRAILGO_ACCOUNT_DELETION_URL=https://your-domain.example/account-delete
```

빌드와 테스트:

```powershell
cd app2
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

디버그 APK는 `app2/app/build/outputs/apk/debug/` 아래에 생성됩니다.

### Spring 서버 실행

서버 설정은 환경변수로 주입합니다. 아래 이름만 참고하고 실제 값은 비밀 관리 도구 또는 배포 환경에 등록하세요.

| 환경변수 | 용도 |
| --- | --- |
| `DB_URL` | MariaDB JDBC 연결 문자열 |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `TRAILGO_ADMIN_USERNAME` | 위험지역 검수 관리자 ID |
| `TRAILGO_ADMIN_PASSWORD_HASH` | BCrypt 형식의 관리자 비밀번호 해시 |
| `TRAILGO_KAKAO_MAP_APP_KEY` | 관리자 지도 화면용 Kakao 키 |
| `TRAILGO_ANDROID_DOWNLOAD_URL` | 웹 소개 페이지의 Android 다운로드 URL |
| `TRAILGO_SUPPORT_EMAIL` | 고객지원 이메일 |
| `TRAILGO_STORAGE_ROOT` | 서버 파일 저장 루트 |

컴파일과 실행:

```powershell
cd web/bikeapp
.\gradlew.bat compileJava
.\gradlew.bat bootRun
```

기본 포트는 `8002`입니다. 운영 환경에서는 HTTPS와 정식 도메인을 사용하세요.

## 공개 API

게스트 MVP에서 앱이 비회원으로 접근하는 API는 읽기 전용입니다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/api/public/config` | 웹 소개 페이지 설정 |
| `GET` | `/api/public/courses` | 공개 코스 목록 |
| `GET` | `/api/public/hazards` | 승인·활성·미만료 위험지역 |

관리자 위험지역 검수 페이지는 `/admin` 아래에 있으며 관리자 인증이 필요합니다.

## 보안 원칙

- DB 접속 정보, 관리자 비밀번호, 지도·외부 API 키를 소스에 하드코딩하지 않습니다.
- 관리자 비밀번호는 BCrypt 해시로만 설정합니다.
- 공개 API는 읽기 전용 공개 코스와 승인된 위험지역으로 제한합니다.
- 레거시 회원·기록 API는 새 인증·소유권 검증을 완료하기 전 앱에서 사용하지 않습니다.
- 비공개 GPX와 회원 기록은 추후 인증된 본인만 접근할 수 있는 `/api/v1/me` 계약으로 이전합니다.

## 향후 로드맵

1. 실제 기기 장시간 라이딩 테스트: GPS, 화면 잠금, 앱 전환, GPS OFF/ON, 배터리 절약 모드
2. 게스트 저장 실패·저장공간 부족·앱 강제 종료 복구 검증
3. 카카오 로그인 도입 후 네이버 로그인 추가
4. 소셜 로그인 검증 후 TrailGO 전용 access/refresh token 발급
5. `/api/v1/me`, `/api/v1/me/rides` 기반 회원 프로필·기록 동기화
6. 사용자가 동의한 게스트 기록만 서버에 동기화
7. HTTPS, 비밀값 교체, DB 백업·복구, 개인정보처리방침 운영 정보 최종 점검

## 배포 전 체크리스트

- [ ] Google Play 배포 서명 기준의 패키지명·키 해시가 지도/로그인 공급자 콘솔에 등록되어 있다.
- [ ] 운영 서버가 HTTPS로 제공된다.
- [ ] 모든 비밀값은 환경변수 또는 배포 비밀 관리에만 저장된다.
- [ ] 노출 가능성이 있는 기존 키·비밀번호를 교체했다.
- [ ] 관리자 계정과 위험지역 검수 권한을 확인했다.
- [ ] 실제 기기에서 장시간 GPS 기록과 GPX 저장·조회가 검증됐다.
- [ ] 개인정보처리방침·이용약관·고객지원 연락처가 실제 운영 정보와 일치한다.

## 라이선스

배포 및 사용 정책은 TrailGO 운영 정책에 따릅니다.
