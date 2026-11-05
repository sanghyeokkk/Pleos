# Gleo Interface 앱 연동 안내

이 문서는 Pleos Gleo Interface 공개 가이드를 현재 프로젝트에 맞춰 요약합니다. 확인일: 2026-10-29. Gleo AI가 앱 기능을 호출할 수 있도록 App Schema를 작성하고 Android Intent를 수신한 뒤, 처리 결과를 Callback Schema 형태로 반환하는 흐름입니다. 실제 차량·Gleo 버전과 앱 심사 조건은 원문과 대상 환경에서 확인합니다.

- [기본 개념](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts)
- [App Schema 작성](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)
- [Callback Schema 작성](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback)
- [Android 코드 예시](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples)
- [스키마 전달 및 등록](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)

## 현재 프로젝트 상태

**2026-11-03 업데이트:** 차계부에 `ADD_MAINTENANCE_RECORD`, `GET_MAINTENANCE_RECORD`, `GET_MAINTENANCE_DUE` 세 동작을 추가했습니다. 실제 음성 호출·응답과 스키마 승인은 아직 검증하지 않았으며, 사용자 확인 없는 저장은 지원하지 않습니다. 현재 구현과 제한은 [내 차 관리 안내](../car-care/README.md)를 기준으로 확인합니다. 아래 기존 상태 설명은 연동 초안 이전의 기록입니다.

현재 `mobile` 앱의 applicationId는 `com.example.myapplication`, 개발용 versionName은 `1.0`입니다. Gleo가 호출할 실제 기능, 마켓에 등록할 앱 버전, 스키마 승인 여부는 아직 정해지지 않았습니다. 기존 앱은 도어 위치 조회와 SDK 초기화 예제를 제공하지만 Gleo Action 처리와 응답 전송은 아직 구현하지 않았습니다.

이 문서 폴더의 [schema.example.json](schema.example.json)은 기존 왼쪽 앞문 위치 조회를 바탕으로 만든 **검토용 초안**입니다. 앱의 assets에 포함되지 않으며, 이 파일만으로 Gleo 명령이 등록되거나 동작하지 않습니다. 실제 기능과 버전이 확정된 뒤 스키마, 수신 컴포넌트, Intent 처리와 콜백을 함께 구현하고 검증합니다.

## 1. 연동 흐름

1. 앱에서 제공할 음성 호출 기능과 입력값, 처리 결과를 정하고 App Schema에 정의합니다.
2. Gleo AI가 사용자 발화를 해석해 해당 기능의 Gleo Action을 Android Intent로 전달합니다.
3. 앱이 지정된 Activity 또는 BroadcastReceiver에서 요청을 검증하고 기능을 수행합니다.
4. 앱은 Callback Schema에 맞춘 결과를 Gleo AI에 전달하고, Gleo AI가 사용자 응답을 구성합니다.

이 가이드에서 `component`가 `service`인 호출은 Android Service 시작이 아니라 `sendBroadcast()` 방식으로 설명됩니다. UI가 필요한 경우 `activity`, 화면 없이 처리하는 경우 `service`를 사용하도록 안내합니다. 사용 중인 플랫폼의 정확한 전달 형식과 보안 요구사항은 실제 환경에서 확인합니다.

## 2. App Schema

- `packageName`: 호출 대상 Android 앱의 applicationId.
- `appDescription`: 앱의 목적을 간결하게 설명하는 영문 문장.
- `version`: Pleos 앱 마켓에 등록된 앱 버전. 현재 Gradle 값만으로 마켓 등록 여부를 판단할 수 없습니다.
- `actionCalls`: 음성으로 호출할 함수 목록이며 문서 기준 최대 3개입니다.
- 각 항목의 `type`은 `function`이고, `function`에는 `name`, `description`, `component`, `parameters`를 정의합니다.
- 함수 이름은 중복되지 않는 영문 대문자와 밑줄을 사용합니다. 설명은 영어로 쓰고 목적과 기능을 간결하게 표현합니다.
- `parameters`는 `object`이며 `properties`와 `required`로 입력을 정의합니다. 문서에 나열된 입력 타입은 `string`, `number`, `boolean`, `array`입니다. 다중 타입 배열과 `null` enum은 지원하지 않는다고 안내합니다. 필수 값이 누락될 수 있으므로 앱에서도 검증해야 합니다.

참고: 이 스키마는 일반 JSON Schema 전체 기능을 지원한다고 가정하지 않습니다. 자세한 제약과 최신 규칙은 [App Schema 작성 가이드](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability)를 확인합니다.

## 3. Intent 수신

문서는 Gleo Action의 함수 이름이 Intent action이 되고, 패키지명이 대상 앱을 지정하며, 입력값이 Intent extras로 전달되는 흐름을 설명합니다. 예시 코드는 `ACTION_PARAM`에 JSON 문자열을 담아 파싱하지만, 같은 문서의 설명에는 개별 extra로 전달된다는 표현도 있어 **실제 SDK·플랫폼 버전에서 전달 형식을 확인해야 합니다**. `onCreate`뿐 아니라 이미 실행 중인 Activity가 새 Intent를 받는 경우도 고려합니다.

외부 Intent에 들어 있는 action과 파라미터는 검증하고, 구현된 기능의 권한·주행 제한·사용자 확인 요구사항을 충족한 뒤 처리합니다. 컴포넌트의 노출 범위와 호출자 검증은 대상 플랫폼 정책에 맞춰 설계합니다. `ACTION_PARAM` 전체나 차량 상태·위치 등 민감한 값을 불필요하게 로그에 남기지 않습니다.

원문: [Code Details with Examples](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples)

## 4. Callback Schema와 응답

| 필드 | 의미 |
| --- | --- |
| `functionName` | 실행한 App Schema의 함수 이름과 일치하는 값 |
| `status` | 성공 `success`, 추가 정보·확인 필요 `asking`, 실패 `fail` |
| `message` | 결과를 설명하는 필수 영문 문장. 검색어·장소명 등 필요한 콘텐츠는 다른 언어일 수 있음 |
| `mode` | 선택값 `voice`, `beep`, `silent`; 생략 시 `voice` |
| `data` | 선택값. 필요한 결과만 담은 JSON 객체 배열을 문자열로 표현 |

문서의 예시는 action `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE`에 `packageName`과 JSON 문자열인 `response` extras를 넣어 브로드캐스트합니다. 원문은 Action을 7초 후 취소한다고 안내하므로 비동기 SDK 결과, 실패와 시간 제한을 처리하고 각 요청에 적절한 결과를 반환해야 합니다. 플랫폼별로 실제 응답 대상과 보안 요구사항을 확인합니다.

원문: [Callback Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback), [Callback 전송 예시](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples)

## 5. 실제 등록 절차

1. 사용자가 요청할 기능과 실제 배포 applicationId·마켓 버전을 확정합니다.
2. 검증된 App Schema를 해당 앱 모듈의 `src/main/assets/schema.json`에 넣습니다. 파일 이름은 `schema.json`이어야 합니다.
3. 각 함수에 맞는 Android Activity 또는 Receiver와 action intent filter를 등록하고, 파라미터 검증과 비동기 처리·Callback 전송을 구현합니다.
4. 스키마, Intent 전달 형식, 앱 실행 상태별 진입, 오류와 시간 제한을 Pleos 환경에서 확인합니다.
5. Pleos의 스키마 검토 및 승인 절차와 배포 조건을 확인합니다. 로컬 파일 생성만으로 외부 등록·승인이 완료되었다고 판단하지 않습니다.

문서는 Gleo AI의 부팅 및 앱 설치·삭제 시 스키마를 읽고 갱신하는 흐름을 설명합니다. 구체적인 감지 시점과 재설치·재부팅 필요 여부는 실제 플랫폼에서 검증합니다. [스키마 전달](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema)

## 검토 목록

- [ ] 노출할 기능을 최대 3개로 정하고 사용자 명령과 입력값을 정의했다.
- [ ] 앱 패키지명과 마켓 등록 버전을 확인했다.
- [ ] 스키마의 이름, 영어 설명, 타입과 필수 입력이 구현과 일치한다.
- [ ] Intent 입력 검증, 컴포넌트 노출과 플랫폼 보안 요구사항을 검토했다.
- [ ] 성공·추가 정보 요청·실패 응답과 7초 제한을 처리했다.
- [ ] 주행 제한, 차량 SDK 권한 및 실제 대상 환경의 동작을 검증했다.

[앱 개발 가이드](../pleos-development/README.md) · [앱 설계·UI 가이드](../pleos-design/README.md)
