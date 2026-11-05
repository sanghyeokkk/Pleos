# Pleos SpeechToText SDK 개발 안내

Pleos 공개 문서의 SpeechToText SDK 3.3.1 안내를 이 프로젝트에 맞춰 정리했습니다. 확인일: 2026-10-29. SDK는 음성을 텍스트로 변환하며, 텍스트에 따른 앱 동작은 별도로 구현해야 합니다. 실제 지원 범위와 앱 등록 조건은 대상 차량과 서비스에서 확인합니다. [SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)

## 프로젝트 설정과 현재 적용 범위

- 기존 Pleos Maven 저장소를 사용하고, `mobile/build.gradle.kts`에 `ai.pleos.playground:SpeechToText:3.3.1`을 추가했습니다. [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk)
- `mobile/src/main/AndroidManifest.xml`에 `pleos.car.permission.STT_SERVICE`를 선언했습니다. 서비스 초기화, 리스너 등록, 앱 등록 및 인식 요청에 필요한 권한입니다. [권한 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions)
- 현재 앱은 `ON_DEVICE` 모드의 초기화를 시도하고 성공하면 결과 리스너를 등록합니다. 화면에 초기화 결과를 표시하고 Activity 종료 시 같은 리스너를 제거한 뒤 SDK를 해제합니다.
- 현재 앱은 음성 인식을 시작하는 `request()`, 외부 음성 데이터를 넘기는 `sendAudio()`, 언어 변경 및 서버 앱 등록을 호출하지 않습니다. 인식 결과 이벤트를 실제 앱 기능에 연결하는 작업도 아직 구현하지 않았습니다.
- 이 PC의 Pleos AVD는 마이크 입력이 비활성화되어 있습니다. 기기 내 SDK 초기화가 성공하더라도 마이크 음성 인식이 가능하다는 의미는 아닙니다.

## 인식 모드와 서버 등록

| 모드 | 동작 | 앱 등록 |
| --- | --- | --- |
| `ON_DEVICE` | 차량 또는 기기의 엔진에서 인식 | 서버 미사용 시 불필요 |
| `SERVER` | 원격 Speech Service에서 인식 | 네트워크와 Pleos Playground의 Client ID·Client Secret 필요 |
| `HYBRID` | 서버를 우선 사용하고 네트워크 연결 실패 시 기기 엔진으로 전환 | 서버 기반 인식 사용 시 필요 |

모드를 생략하면 `HYBRID`가 기본입니다. 서버 기반 인식은 음성 데이터를 원격 서비스로 전송하므로 앱의 개인정보 안내와 데이터 처리 정책을 함께 검토합니다. 초기화 후 `registerApp()`으로 등록하고 서버 연결 성공 이벤트를 확인한 뒤 서버 인식을 요청합니다. 자격 증명은 소스 코드나 README에 기록하지 않습니다. [앱 등록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/app-registration)

## 초기화와 결과 리스너

1. 애플리케이션 컨텍스트와 필요한 모드로 `SpeechToText` 객체를 만듭니다.
2. `initialize(OnInitializeListener)`의 성공·실패 콜백을 처리합니다.
3. 초기화 후 `ResultListener`를 등록하고 요청에 필요한 결과와 오류를 처리합니다.
4. 사용이 끝나면 같은 리스너를 제거하고 `release()`합니다.

`ResultListener`의 주요 이벤트는 준비(`onReady`), 인식 시작·종료(`onStartedRecognition`, `onEndedRecognition`), 중간·완료 텍스트(`onUpdated`), 음성 구간(`onUpdatedEpdData`), 입력 크기(`onUpdatedRms`), 오류(`onError`)입니다. `completed`가 참이면 해당 인식 결과가 완료된 것이며, EPD의 위치는 입력 버퍼의 바이트 오프셋입니다. RMS는 신호 크기이며 dB로 단정하지 않습니다. 현재 앱은 오류만 화면에 표시하고 나머지 이벤트는 인식 기능을 구현할 때 연결하도록 비워 두었습니다.

원문: [초기화와 해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release), [결과 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners)

## 언어와 음성 인식 시작·중지

`setLocale()`로 인식 요청 전에 언어와 지역을 선택할 수 있습니다. 문서에 제시된 로케일은 `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`입니다. 명시하지 않으면 같은 앱의 이전 설정을 우선하고, 이전 값이 없으면 시스템 로케일을 사용합니다. 선택한 모드와 엔진의 실제 언어 지원도 확인합니다. [언어 설정](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale)

차량 마이크 인식은 SDK 초기화와 리스너 등록을 완료한 뒤 `request()`로 시작하고 `stop()`으로 마칩니다. 문서에 따르면 `request()`는 마이크를 활성화합니다. 서버 기반 모드에는 해당 앱 등록 또는 인증 절차가 필요합니다. 실제 녹음 기능 구현 시 사용자 동의, 필요한 권한과 마이크 접근 상태를 확인해야 합니다. [인식 시작·중지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop)

## PCM 오디오 데이터 입력

차량 마이크 대신 앱이 보유한 PCM 데이터를 사용하는 `sendAudio(ByteArray)`는 문서상 `ON_DEVICE` 모드에서 사용합니다. 데이터를 한 번에 보내거나 여러 조각으로 나눈 뒤 마지막에 `completeAudioSend()`로 입력 종료를 알리고, 등록한 리스너에서 인식 결과를 받습니다. 문서의 3,200바이트/100ms는 16kHz·16bit·mono PCM에 대한 예시이므로 다른 형식에는 그대로 적용하지 않습니다. 실제 지원 PCM 형식과 호출 조건은 대상 SDK·서비스에서 확인합니다. [오디오 데이터 전송](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data)

## 오류 처리

초기화·앱 등록·인식 오류는 각각의 리스너로 `ErrorType`을 전달합니다. 마이크 입력, PCM 데이터, 호출 상태, 권한, 언어, 네트워크·시간 초과, 서버 인증, 기기 엔진 및 서비스 오류를 구분합니다. 새로운 오류 값에 대비해 기본 분기를 둡니다. 현재 앱은 오류 코드 이름을 보여 주며, 인식 UI를 구현할 때 상황별 복구 안내를 추가합니다. [오류 코드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/error-code)

## API 패키지와 주요 클래스

전체 항목은 [SpeechToText API Reference](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/entry)를 확인합니다. SDK 의존성을 추가하면 사용하려는 클래스를 가져와 사용할 수 있습니다.

| 패키지 | 클래스·역할 | 원문 |
| --- | --- | --- |
| `ai.pleos.playground.stt` | `SpeechToText`: SDK 진입점과 요청 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/entry) · [클래스](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/entry) |
| `ai.pleos.playground.stt.constant` | `Mode`, `ErrorType`: 인식 모드와 오류 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.constant/entry) · [Mode](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.constant/mode/entry) · [ErrorType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.constant/error-type/entry) |
| `ai.pleos.playground.stt.data` | `InferenceSource` 등 데이터 모델 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.data/entry) · [InferenceSource](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.data/inference-source/entry) |
| `ai.pleos.playground.stt.listener` | 초기화, 서버 연결, 인식 결과 리스너 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.listener/entry) · [OnInitializeListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.listener/on-initialize-listener/entry) · [OnServerConnectionListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.listener/on-server-connection-listener/entry) · [ResultListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt.listener/result-listener/entry) |

### SpeechToText 클래스의 API 목차

- [Companion](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/companion/entry) · [생성자](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/-speech-to-text)
- [initialize](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/initialize) · [release](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/release)
- [addListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/add-listener) · [removeListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/remove-listener)
- [registerApp](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/register-app) · [setLocale](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/set-locale)
- [request](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/request) · [stop](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/stop) · [sendAudio](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/send-audio) · [completeAudioSend](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/speech-to-text/ai.pleos.playground.stt/speech-to-text/complete-audio-send)

생성자, `removeListener`, `OnServerConnectionListener`의 상세 페이지는 조회 도구에서 내용을 가져오지 못해 목차에 표시된 주소만 기록했습니다. 실제 인자와 동작은 공식 문서 또는 설치된 SDK에서 확인합니다.

## 구현·검증 체크리스트

- [ ] 서비스 초기화·결과 리스너 등록과 해제를 앱 생명주기에 맞게 처리했다.
- [ ] 사용자에게 인식 기능과 데이터 처리 방식을 알리고 필요한 권한·장치 상태를 확인했다.
- [ ] 서버 모드를 사용할 경우 앱 등록과 자격 증명 보관, 연결 실패를 처리했다.
- [ ] 언어·입력 소스·오류에 맞는 UI를 구현하고 대상 차량과 에뮬레이터에서 검증했다.
- [ ] 인식한 텍스트를 사용해 실제 기능을 실행한다면 의도 확인과 주행 제한을 별도로 처리했다.

[앱 개발 가이드](../pleos-development/README.md) · [TextToSpeech 안내](../pleos-text-to-speech/README.md)
