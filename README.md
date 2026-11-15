# Car maintenance (내 차 관리)

차량의 누적 주행거리와 정비 이력을 바탕으로 다음 점검 시기를 확인하고, Gleo AI와 대화해 정비 기록을 남기는 차량용 앱 프로토타입입니다.

## 주요 기능

- **정비 기록:** 항목, 정비 날짜, 당시 주행거리, 점검 거리·기간 주기와 비용을 저장하고 수정·삭제합니다. 점검 주기는 차량 취급설명서와 정비 이력을 확인해 직접 설정합니다.
- **Gleo 음성 기록:** 항목과 날짜, 주기 등을 말하면 기록 내용을 확인한 후 저장합니다. 확인 단계에서 주기를 정정할 수 있습니다. 오늘 정비한 기록에 당시 거리를 말하지 않으면 차량 값을 조회해 확인 내용에 넣습니다.
- **음성 조회:** 저장한 항목의 최근 정비 기록과 점검 현황을 조회합니다. 거리 기준 결과는 차량 거리와 정비 당시 거리를 사용할 수 있을 때 계산합니다.
- **차량 연동:** Pleos Vehicle SDK로 누적 주행거리를 읽고, 기어와 차량 UI 제한을 확인합니다. 기록 수정과 정비소 검색은 주차 및 입력 허용 상태에서만 가능합니다.
- **알림과 정비소:** 사용자가 켠 경우 약 하루마다 점검 시기를 확인합니다. 주변 정비소 후보 검색과 Pleos 내비게이션의 블루핸즈 검색 화면을 제공합니다.

정비 기록은 앱이 설치된 기기에 저장됩니다. 소모품의 실제 마모를 진단하거나 제조사 정비 주기를 자동으로 판정하지 않습니다.

## 제출 APK

- 파일: [`apk/Pleos_Car maintenance_release.apk`](apk/Pleos_Car%20maintenance_release.apk)
- `mobile` 모듈의 release 빌드를 공모전 시연용 키로 서명한 APK입니다. 패키지 ID는 `com.example.myapplication`, 버전은 `1.0`입니다.
- SHA-256: `6D75B3C3F0A3D8B7F277DD2750380FF41E36A5A449F25A0B521214EEE169C775`
- 같은 패키지의 디버그 APK가 이미 설치되어 있고 서명 키가 다르면 업데이트 설치가 거부됩니다. 기존 기기의 데이터를 보존하려면 앱을 삭제하지 말고 별도의 깨끗한 테스트 에뮬레이터에서 설치하세요.
- 서명 키와 비밀번호는 저장소에 포함하지 않습니다. 향후 동일 패키지의 업데이트를 배포하려면 같은 서명 키를 안전하게 보관해야 합니다.

## 테스트 환경 및 실행 방법

| 항목 | 환경 |
| --- | --- |
| 실행 대상 | Pleos Connect Emulator v2.1.0, Android API 34, x86_64 |
| 테스트 화면 | 전체 에뮬레이터 화면 2560 × 1440 px, 앱은 차량 UI의 분할 화면에서 실행 |
| 개발 환경 | Android Studio, JDK 21, Gradle Wrapper 9.6.0, Android Gradle Plugin 9.4.1 |
| 빌드 | `mobile` 모듈, compile/target SDK 37, 최소 SDK 28 |

1. Android Studio에서 저장소 루트를 엽니다. Android SDK와 Pleos Connect Emulator v2.1.0을 설치하고 Gradle을 동기화합니다. 처음 빌드할 때 Google·Maven Central·Pleos Maven 저장소에 접근해야 합니다.
2. Pleos Connect AVD를 실행합니다. Gleo 음성 시연에는 인터넷 연결, PC 마이크 및 에뮬레이터의 **Extended Controls → Microphone → Enable Host Microphone Access** 설정이 필요합니다. 날짜가 다르면 에뮬레이터 시간대를 서울로 설정합니다.
3. APK를 설치하거나 Android Studio에서 `mobile` 구성을 실행합니다. 예를 들어 깨끗한 에뮬레이터에서 아래 명령을 사용합니다.

   ```powershell
   adb install "apk/Pleos_Car maintenance_release.apk"
   ```

4. 차 관리 앱에서 기어가 `P`이고 입력 제한이 해제된 상태인지 확인합니다. 차량 상태와 누적거리는 Pleos Connect Virtual Console에서 시험할 수 있습니다.
5. Gleo 호출어는 한국어 **내 차 관리**입니다. Gleo를 호출해 다음과 같이 시험합니다.

   - “내 차 관리에 오늘 엔진오일을 1만 km 주기로 기록해 줘.”
   - 확인 질문에서 “아니, 5천 km로 바꿔 줘.”라고 정정한 뒤 내용을 확인하고 저장을 승인합니다.
   - “내 차 관리에서 엔진오일 기록 알려줘.”라고 조회합니다.

Windows에서 소스 빌드 및 단위 테스트는 다음과 같이 실행할 수 있습니다.

```powershell
.\gradlew.bat :mobile:assembleDebug :mobile:testDebugUnitTest
```

## 구현·시험 범위

- Pleos Vehicle 2.3.2, NaviHelper 3.0.5, SpeechToText·TextToSpeech 3.3.1, FusedLocation 1.1.3을 사용합니다. Gleo 연동 스키마는 `mobile/src/main/assets/schema.json`에 있습니다.
- Pleos Connect v2.1.0 에뮬레이터에서 실제 음성으로 기록 추가·주기 정정·저장과 항목 조회를 확인했습니다. 앱 프로세스 종료 후 실제 음성 조회도 확인했습니다. 종료 상태의 음성 추가는 Gleo 요청을 재현한 자동 테스트로 확인했으며 실제 발화는 재시험하지 않았습니다.
- 서명된 release APK를 별도 Pleos Connect v2.1.0 에뮬레이터에 설치해 기본 화면을 확인했으며, release APK를 대상으로 한 저장·음성 요청·조회 자동 테스트 6개가 통과했습니다. release APK의 실제 마이크 음성 호출은 별도로 시험하지 않았습니다.
- 거리·날짜 기준 알림, 저장과 조회는 기존 사용자 데이터와 분리된 테스트 기록으로 확인했습니다. 알림의 실제 정기 실행 시각과 재부팅 후 동작은 추가 확인이 필요합니다.
- **실차에서 Gleo 권한, 주행거리 값·단위와 차량별 주행 제한은 아직 검증하지 않았습니다.** 앱 마켓용 Gleo 스키마 승인 여부도 별도 확인이 필요합니다.
- 일반 정비소 검색은 사용자가 요청할 때 현재 위치를 공개 OpenStreetMap Overpass 서버에 전송합니다. 이 서버는 시제품 시험용이며 가용성을 보장하지 않습니다. 일반 정비소 경로 요청의 실제 성공 여부는 미확인입니다.
