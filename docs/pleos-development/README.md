# Pleos Connect 앱 개발 및 Vehicle SDK 안내

이 문서는 Pleos 공개 문서를 바탕으로 이 프로젝트의 앱 개발과 Vehicle SDK 연동에 필요한 내용을 정리합니다. 확인일: 2026-10-29. 앱의 카테고리, 배포 형태, 차량 모델과 플랫폼 버전에 따라 적용 조건이 달라질 수 있으므로 배포 및 심사 전 원문을 다시 확인합니다.

- [Pleos Connect 앱 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro)
- [앱 설계 원칙과 UI 디자인 정리](../pleos-design/README.md)
- [NaviHelper SDK 개발 안내 및 API 목록](../pleos-navihelper/README.md)
- [TextToSpeech SDK 개발 안내](../pleos-text-to-speech/README.md)
- [SpeechToText SDK 개발 안내 및 API 목록](../pleos-speech-to-text/README.md)
- [FusedLocation SDK 개발 안내 및 API 목록](../pleos-fused-location/README.md)
- [Gleo Interface 앱 연동 안내](../pleos-gleo/README.md)

## 1. 앱 개발 가이드

### 오디오 포커스

차량에서는 음악, 내비게이션, 통화, 안전 안내 등 여러 소리가 충돌할 수 있습니다. 오디오를 재생하는 앱은 콘텐츠의 속성에 맞게 포커스를 요청하고, 획득·거부·상실·일시 상실에 따라 재생 중단, 볼륨 조정, 재개 가능 여부를 처리합니다. 실제 상호작용은 Pleos Connect 오디오 포커스 정책과 원문의 매트릭스를 기준으로 확인합니다.

Suspend 진입 시 일반 오디오 재생을 중단하고 보유한 포커스를 반납하며 필요한 상태를 저장합니다. Resume 후에는 자동으로 포커스가 돌아온다고 가정하지 않고 필요 시 다시 요청합니다. 안전·긴급 등 예외는 시스템 정책을 따릅니다.

- [오디오 포커스 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)
- [Suspend 모드 오디오 포커스](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus)

### 주행 규제

`CarUxRestrictionsManager`에서 현재 제한과 변경 알림을 받아 각 기능의 동작과 화면에 반영합니다. Distraction Optimization을 선언한 앱도 활성 제한을 확인하고 해당 기능을 제한해야 합니다. 영상, 키보드, 검색 등의 제한을 지역별 고정값으로 추측하지 말고 플랫폼이 제공하는 제한 상태에 맞춥니다.

원문: [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)

### CCP 입력

CCP는 다이얼, 로터리 컨트롤러, 물리 버튼 등을 포함하는 차량 입력 장치입니다. 사용자 설치 앱은 CCP 지원을 선택할 수 있지만, 지원하지 않을 때도 필수 기능을 터치로 사용할 수 있어야 합니다. 기본 탑재 앱에는 CCP 지원 의무와 예외 조건이 별도로 안내됩니다. 적용 여부는 앱의 실제 배포 형태 및 원문의 심사 기준으로 확인합니다.

원문: [CCP 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ccp-rotary-controller-development)

### Maps/Navigation 앱

내비게이션 앱은 안전을 우선하고 주행 중 조작을 최소화합니다. 앱의 실제 기능과 매니페스트·마켓 등록 카테고리가 일치해야 하며, 경로 안내의 단일 활성 경로 정책과 포그라운드·백그라운드 안내 조건을 확인합니다. 내비게이션 카테고리 선언은 해당 기능을 제공하는 앱에 적용합니다.

원문: [Maps/Navigation 앱 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide)

### Music/Audio 앱

음악, 라디오, 팟캐스트 또는 오디오북처럼 오디오 재생이 중심인 앱은 카테고리 기준과 차량 미디어 컨트롤, 백그라운드 재생, 미디어 라이브러리 연동 및 콘텐츠 분류를 확인합니다. 영상이 주 기능인 앱은 해당 카테고리의 대상으로 설명되지 않습니다. 카테고리와 미디어 라이브러리 관련 정책은 배포 심사에도 사용됩니다.

원문: [Music/Audio 앱 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/music-audio-app-developement-guide)

### TTS와 음성 안내

음성 출력 시작부 손실을 줄이기 위해 문서는 안내 시작 전 150~200 ms 선행 묵음을 권장합니다. 가능한 경우 오디오 데이터 앞에 묵음을 넣고, 연속된 하나의 발화에는 불필요한 묵음이 반복되지 않도록 합니다. 선행 묵음은 오디오 경로 초기화와 포커스 확보를 대신하지 않습니다.

원문: [TTS / 음성 안내 선행 묵음 적용 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide)

### 키보드와 창 크기

입력 필드가 있는 창은 키보드가 표시될 때 해당 창이 밀려 올라갈 수 있습니다. 입력 필드가 없는 창이 키보드 때문에 불필요하게 이동하지 않도록 해야 하는 경우, 원문에서 설명하는 IME insets 처리와 적용 대상을 확인합니다. 이 예제 화면에는 현재 텍스트 입력 필드가 없습니다.

원문: [IME 동작에 따른 window resize 처리 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ime-window-resize-handling-guide)

### 플랫폼 확인과 디버깅

- 실차 플랫폼 식별 값과 지역 코드, 내비게이션 탑재 여부, 차량 속도·배터리 정보, 플랫폼별 미디어 코덱 지원에 관한 안내를 확인합니다. 에뮬레이터의 플랫폼 식별 또는 코덱 결과를 실차의 결과로 가정하지 않습니다.
- ADB로 연결 기기 확인, 앱 설치·실행, 로그 확인, 화면 캡처와 주행 제한 디버깅 등을 할 수 있습니다. 주행 제한을 변경하는 디버깅 명령은 실제 사용자 차량이 아닌 적절한 테스트 환경에서 원문 지침에 따라 사용합니다.
- 이 PC에서 `adb`가 PATH에 없으면 PowerShell에서 `& "D:\Android\Sdk\platform-tools\adb.exe" devices`처럼 전체 경로를 지정할 수 있습니다.

원문: [앱 실행 환경 및 플랫폼 리소스 FAQ](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq), [ADB 명령어 FAQ](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide)

## 2. Vehicle SDK

### 설치와 권한

현재 프로젝트의 `settings.gradle.kts`에 Pleos Maven 저장소가 등록되어 있고, `mobile/build.gradle.kts`는 Vehicle SDK `2.3.2`를 사용합니다. Pleos 개발 문서도 이 버전을 기준으로 안내합니다. SDK를 호출하는 `mobile` 앱의 매니페스트에 도어 조회용 `pleos.car.permission.CAR_DOORS`가 선언되어 있습니다.

`CAR_*` 계열은 조회, `CONTROL_CAR_*` 계열은 제어에 주로 사용됩니다. 실제 필요한 권한은 도메인명만으로 추측하지 말고 호출할 API의 권한 명세를 확인해 필요한 것만 선언합니다. 앱 심사와 권한 사용 범위에 관한 Pleos 정책도 별도로 적용됩니다.

원문: [SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup), [권한 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)

### 객체와 생명주기

`Vehicle`을 생성하고 `initialize()`를 호출한 뒤 `getDoor()`처럼 SDK 진입점에서 도메인 객체를 얻습니다. 차량 서비스 호출 결과는 비동기 콜백으로 전달됩니다. SDK가 더 이상 필요하지 않을 때 `release()`를 호출하며, 진행 중인 콜백의 처리와 Activity 종료 시점을 고려해야 합니다.

원문: [도메인 객체 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object)

### 네 가지 API 유형

| 유형 | 목적 | 구현 시 확인할 점 |
| --- | --- | --- |
| Capability | 기능의 지원 여부, 접근 유형 및 영역별 값 범위 확인 | 대상 차량·영역의 읽기 또는 쓰기 지원을 확인하고 확인 실패와 미지원을 구별합니다. |
| Get | 현재 값 조회 | `onComplete`와 `onFailed`를 처리하고 값이 `null`일 수 있음을 고려합니다. |
| Set | 차량 상태 또는 설정 변경 | 제어 권한, 해당 영역의 쓰기 가능 여부, 허용 범위와 주행 제한을 확인합니다. 실제 변경 확인은 해당 API 명세에 따릅니다. |
| Callback | 상태 변화 구독 | 등록한 리스너를 더 이상 사용하지 않을 때 같은 객체와 영역으로 해제합니다. |

원문: [Capability](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability), [Get](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property), [Set](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/set-property), [Callback](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback)

### 오류 처리

비동기 오류는 `Exception`으로 전달됩니다. Pleos 문서는 `VehicleException.errorCode`로 원인을 분류하는 방식을 설명하지만, 현재 프로젝트에서 내려받은 `2.3.2` 라이브러리에서는 이 타입을 공개 이름으로 참조할 때 컴파일되지 않았습니다. 따라서 현재 앱은 확인 또는 조회 단계와 예외 메시지를 표시합니다. SDK 배포본과 문서가 일치하는지 확인한 뒤 오류 코드별 처리를 추가할 수 있습니다.

원문: [Vehicle SDK 오류 코드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code)

## 3. 현재 프로젝트의 적용 범위

- `mobile` 앱은 `Vehicle`을 초기화하고 왼쪽 앞문 위치의 capability, 읽기 접근 및 영역 지원을 확인한 뒤 현재 위치를 조회합니다. 결과 없음과 읽기 미지원은 별도로 표시하고, 확인 또는 조회 실패 시 예외 메시지를 표시하며 다시 조회할 수 있습니다.
- SDK 사용이 끝나는 Activity 종료 시 리소스를 해제하며, 종료된 화면 또는 이전 요청의 콜백이 현재 상태를 덮어쓰지 않도록 처리합니다.
- 현재 예제는 도어 상태를 **읽기만** 합니다. NaviHelper는 최초 연결을 시도하고 오류 이벤트 리스너를 등록하며 종료 시 해제합니다. TextToSpeech와 SpeechToText는 기기 내 모드 초기화와 리스너 등록을 시도합니다. FusedLocation은 지원 여부를 확인하고 지원 환경에서 초기화를 호출합니다. 경로·목적지 조회 및 변경, 음성 재생·인식, 위치 조회 및 런타임 위치 권한 요청은 아직 구현하지 않았습니다. 도어 제어(Set), 상태 변경 구독과 해제(Callback), 오디오 포커스 및 주행 규제 연동도 별도 구현이 필요합니다.
- 실제 차량과 에뮬레이터의 지원 기능은 다를 수 있습니다. 도어 값의 의미와 허용 범위는 해당 차량의 capability 및 API 명세를 확인합니다.

## 4. 개발 전 확인

- [ ] 앱 카테고리와 설치·배포 유형에 해당하는 가이드 및 심사 기준을 확인했다.
- [ ] 필요한 기능의 권한, 지원 여부, 영역과 오류 처리를 확인했다.
- [ ] 주행 제한 및 창 크기·화면 전환에 대한 동작을 별도로 구현하고 검증했다.
- [ ] 오디오, 음성, 미디어 또는 내비게이션 기능을 제공한다면 해당 전문 가이드를 반영했다.
- [ ] 에뮬레이터 검증 후 대상 실차·플랫폼에서 지원 여부와 동작을 다시 확인했다.
