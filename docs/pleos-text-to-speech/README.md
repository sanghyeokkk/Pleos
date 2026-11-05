# Pleos TextToSpeech SDK 개발 안내

Pleos 공개 문서의 TextToSpeech SDK 3.3.1 안내를 이 프로젝트에 맞춰 정리했습니다. 확인일: 2026-10-29. 사용 가능한 기능은 차량, 서비스와 실행 환경에 따라 달라질 수 있으므로 실제 적용 시 원문을 확인합니다.

## 프로젝트 설정

- Pleos Maven 저장소는 프로젝트의 `settings.gradle.kts`에 등록되어 있습니다.
- `mobile/build.gradle.kts`에 `ai.pleos.playground:TextToSpeech:3.3.1` 의존성을 추가했습니다.
- `mobile/src/main/AndroidManifest.xml`에 `pleos.car.permission.TTS_SERVICE`를 선언했습니다. 이 권한은 초기화와 이벤트 등록을 포함한 서비스 접근에 필요합니다.
- 현재 앱은 `Mode.ON_DEVICE`로 초기화를 시도하고 성공하면 이벤트 리스너를 등록합니다. 최초 초기화 결과를 화면에 표시하며, Activity 종료 시 리스너를 제거하고 SDK를 해제합니다. 초기화 성공만으로 실제 음성 재생 성공을 보장하지 않습니다.
- 현재 앱에는 `speak()`, `stop()`, `setLocale()` 호출이나 서버 앱 등록을 위한 자격 증명이 없습니다. 음성 요청 기능과 대상 언어가 정해지면 구현합니다.

원문: [SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro), [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk), [권한 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions)

## 합성 모드와 앱 등록

| 모드 | 동작 | 앱 등록 |
| --- | --- | --- |
| `ON_DEVICE` | 기기의 음성 합성 엔진을 사용 | 서버 기반 기능을 사용하지 않으므로 불필요 |
| `SERVER` | 서버 기반 합성 사용 | 네트워크와 Pleos Playground에서 발급한 Client ID·Client Secret 필요 |
| `HYBRID` | 서버를 우선 사용하고 네트워크 연결 실패 시 기기 엔진으로 전환 | 서버 기반 합성 사용 시 필요 |

모드를 생략하면 `HYBRID`가 기본입니다. 서버 기반 모드에서는 초기화 후 `registerApp()`으로 등록하고 서버 연결의 `onConnected()`를 확인한 다음 음성을 요청합니다. 등록 또는 연결 실패는 `onFailed()`에서 처리합니다. Client Secret은 소스 코드나 README에 기록하지 말고, 프로젝트의 자격 증명 관리 방식을 정한 후 등록을 진행합니다.

원문: [앱 등록](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration)

## 초기화와 이벤트

1. 애플리케이션 컨텍스트와 필요한 모드로 `TextToSpeech` 객체를 만듭니다.
2. `initialize(OnInitializeListener)`에서 성공 또는 오류를 확인합니다.
3. 초기화된 SDK에 `EventListener`를 등록하고 필요한 요청을 처리합니다.
4. SDK 사용을 마치면 같은 이벤트 리스너를 제거한 뒤 `release()`로 리소스를 해제합니다.

이벤트에는 `onReady`, `onStart`, `onUpdatedRms`, `onDone`, `onStop`, `onError`가 있습니다. 현재 앱은 오류 이벤트를 화면에 표시하며, 나머지 이벤트의 구체적인 UI 동작은 음성 기능을 구현할 때 정합니다.

원문: [초기화와 해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release), [이벤트 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners)

## 언어 설정과 음성 요청

`setLocale()`로 합성 전에 언어와 지역을 설정할 수 있습니다. 문서에 열거된 지원 로케일은 `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`입니다. 설정하지 않으면 같은 앱의 이전 설정을 우선 사용하고, 이전 값이 없다면 시스템 로케일을 사용합니다. 실제 엔진에서 해당 언어를 사용할 수 있는지도 확인합니다.

`speak()`는 텍스트를 합성·재생하며 기본적으로 하나의 완성된 요청으로 처리합니다. 여러 조각을 한 발화로 전달할 때는 마지막 조각에서 `isFinal=true`를 사용합니다. `stop()`은 진행 중인 합성과 재생을 중지하며, 중지 이벤트는 `onStop()`으로 받습니다. 서버 기반 요청은 서버 연결 성공 후 시작합니다.

음성 재생을 구현할 때는 차량의 오디오 포커스와 음성 안내 선행 묵음 정책도 검토합니다. 관련 내용은 [앱 개발 가이드](../pleos-development/README.md)를 확인합니다.

원문: [언어 설정](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale), [합성 시작·중지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop)

## 오류 처리

오류는 초기화, 서버 연결 및 재생 이벤트의 `ErrorType`으로 전달됩니다. 문서는 권한 부족, 인증 실패, 네트워크·시간 초과, 오디오 오류, 기기 엔진 오류, 미지원 API·언어 등의 상황을 구분하고, 새 오류가 추가될 수 있으므로 기본 분기를 두도록 안내합니다. 실제 음성 기능 구현 시 상황에 맞는 안내와 복구를 추가합니다.

원문: [오류 코드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code)

## API 패키지와 클래스

TextToSpeech SDK를 Gradle 의존성으로 추가하면 다음 패키지의 클래스를 필요한 곳에서 가져와 사용할 수 있습니다. 전체 목차는 [TextToSpeech API Reference](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/entry)에서 확인합니다.

| 패키지 | 주요 클래스 | 용도 |
| --- | --- | --- |
| [`ai.pleos.playground.tts`](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/entry) | [`TextToSpeech`](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/entry) | SDK 초기화, 등록, 음성 합성과 리소스 해제 |
| [`ai.pleos.playground.tts.constant`](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.constant/entry) | [`Mode`](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.constant/mode/entry), [`ErrorType`](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.constant/error-type/entry) | 기기·서버 합성 모드와 오류 유형 |
| [`ai.pleos.playground.tts.listener`](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.listener/entry) | [`EventListener`](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.listener/event-listener/entry), [`OnInitializeListener`](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.listener/on-initialize-listener/entry), [`OnServerConnectionListener`](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts.listener/on-server-connection-listener/entry) | 합성 이벤트, 초기화 결과, 서버 연결 결과 수신 |

### TextToSpeech 클래스의 API 목차

- [Companion](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/companion/entry) · [생성자](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/-text-to-speech)
- [initialize](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/initialize) · [release](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/release)
- [addEventListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/add-event-listener) · [removeEventListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/remove-event-listener)
- [registerApp](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/register-app)
- [setLocale](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/set-locale) · [speak](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/speak) · [stop](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/stop)
- [setAudioAttributes](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/set-audio-attributes) · [setAudioZoneId](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/set-audio-zone-id) · [setSpeakerType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/text-to-speech/ai.pleos.playground.tts/text-to-speech/set-speaker-type)

일부 상세 API 페이지는 조회 도구에서 내용을 가져오지 못해 주소와 목차만 확인했습니다. 각 인자와 동작은 해당 공식 페이지 또는 위 프로그래밍 가이드에서 다시 확인합니다.

## 확인 목록

- [ ] 대상 차량 또는 에뮬레이터의 Speech Service와 선택한 모드·언어 지원을 확인했다.
- [ ] `TTS_SERVICE` 권한이 매니페스트에 선언되고 실행 환경에서 허용되는지 확인했다.
- [ ] 초기화와 이벤트 등록·해제 및 오류 처리를 앱 생명주기에 맞게 처리했다.
- [ ] 서버 합성을 사용한다면 앱 등록, 자격 증명 보관과 서버 연결을 검증했다.
- [ ] 실제 음성 재생과 중지, 오디오 포커스 및 주행 환경을 테스트했다.

[앱 개발 가이드](../pleos-development/README.md) · [NaviHelper 안내](../pleos-navihelper/README.md)
