# Pleos FusedLocation SDK 개발 안내

이 문서는 Pleos 공개 문서의 FusedLocation SDK 1.1.3 안내를 현재 프로젝트에 맞춰 정리합니다. 확인일: 2026-10-29. 위치는 민감한 정보이므로 실제 기능을 구현할 때 사용자에게 필요한 이유를 설명하고, 허용된 범위에서만 사용합니다.

## 프로젝트 설정과 현재 적용 범위

- `settings.gradle.kts`의 기존 Pleos Maven 저장소를 사용합니다.
- `mobile/build.gradle.kts`에 `ai.pleos.playground:FusedLocation:1.1.3`을 추가했습니다.
- `mobile/src/main/AndroidManifest.xml`에 `pleos.car.permission.FUSED_LOCATION`을 선언했습니다.
- 현재 앱은 `FusedLocation`을 생성해 `isSupported()`를 확인하고, 지원하는 경우 `initialize()`를 호출합니다. Activity 종료 시 SDK를 해제합니다. 초기화 호출이 예외 없이 반환된 것은 위치 데이터 수신 성공을 의미하지 않습니다.
- 현재 앱은 런타임 위치 권한을 요청하지 않으며, 위치 조회·업데이트·근접 알림 API를 호출하지 않습니다. 위치를 수집하거나 표시하는 기능은 아직 구현하지 않았습니다.

원문: [SDK 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro), [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup)

## 위치 권한

`pleos.car.permission.FUSED_LOCATION`은 문서에서 `dangerous` 보호 수준으로 설명하며, 마지막 위치 조회와 위치 업데이트 등록·해제에 필요합니다. **매니페스트 선언만으로 사용자 허용이 완료되는 것은 아닙니다.** 실제 위치 기능을 구현할 때 권한 요청과 허용·거부·재요청 흐름을 앱의 사용 목적에 맞게 구현하고 대상 환경에서 동작을 확인해야 합니다.

차량의 개인정보 설정으로 위치 접근이 차단된 경우 `showLocationAccessBlockedPopup()`은 차단 상태를 안내하는 기능입니다. 이 호출 자체가 위치 접근을 허용하지는 않습니다. 앱에 불필요한 위치 데이터를 저장하거나 로그에 남기지 않도록 합니다.

Android의 `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`과 Google Fused Location 동작을 이 SDK의 보장으로 가정하지 않습니다. 개별 API의 권한과 플랫폼 정책을 확인합니다. [권한 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions)

## 지원 확인과 생명주기

1. `FusedLocation` 인스턴스를 만들고 `isSupported()`로 환경 지원 여부를 확인합니다.
2. 지원되는 경우 `initialize()`를 호출합니다. 지원하지 않으면 위치 기능을 비활성화하거나 다른 흐름을 제공합니다.
3. 권한과 초기화 조건을 충족한 뒤 필요한 위치 API를 호출합니다.
4. 업데이트 리스너를 등록했다면 같은 리스너로 `unregisterFusedLocationCallback()`을 호출합니다.
5. SDK가 더 이상 필요하지 않으면 `release()`를 호출합니다. 해제한 인스턴스를 다시 사용할 때는 새 인스턴스를 만들고 지원 확인부터 다시 진행합니다.

리스너 해제와 SDK 해제는 각각 다른 작업입니다. 현재 앱은 위치 업데이트 리스너를 등록하지 않으므로 SDK만 해제합니다. [초기화 및 해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release)

## 위치 접근과 오류 처리

- `getLastKnownLocation()`은 마지막으로 확보된 위치를 전달하며 새로운 위치를 계산하는 요청이 아닙니다. 데이터가 없으면 결과가 `null`일 수 있습니다.
- `registerFusedLocationCallback()`으로 등록한 `FusedLocationListener`는 `onChanged(LocationData?)`와 `onError(Exception)`으로 결과를 받습니다. 더 이상 필요하지 않을 때 같은 리스너를 해제합니다.
- `LocationData`에는 위도, 경도, 고도, 진행 방향, 신뢰도와 타임스탬프가 포함됩니다. 정확한 타입과 의미는 API 참조를 확인합니다.
- `addProximityAlert()`와 `removeProximityAlert()`는 설정 영역의 진입·이탈 알림에 사용됩니다. 요청 값과 필요한 권한은 API별 명세 및 실제 환경에서 확인합니다.
- `null`은 유효한 위치로 처리하지 않고, 오류 및 접근 차단 상태에서는 위치를 사용할 수 없다는 안내와 적절한 대체 흐름을 제공합니다.

원문: [위치 데이터 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access), [오류 처리](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/error-handling)

## API 패키지와 주요 클래스

전체 목차는 [FusedLocation API Reference](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/entry)를 확인합니다.

| 패키지 | 클래스와 용도 | 원문 |
| --- | --- | --- |
| `ai.pleos.playground.location.fused` | `FusedLocation`: 지원 확인, 초기화, 위치 요청, 해제 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/entry) · [FusedLocation](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/entry) |
| `ai.pleos.playground.location.fused.listener` | 위치와 근접 알림 결과 리스너 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.listener/entry) · [FusedLocationListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.listener/fused-location-listener/entry) · [ProximityAlertListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.listener/proximity-alert-listener/entry) |
| `ai.pleos.playground.location.fused.model` | 위치 데이터와 근접 알림 모델 | [패키지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/entry) · [LocationData](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/location-data/entry) · [ProximityAlertEvent](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-alert-event/entry) · [ProximityAlertRequest](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-alert-request/entry) · [ProximityTransition](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused.model/proximity-transition/entry) |

### FusedLocation 클래스의 API 목차

- [생성자](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/-fused-location) · [isSupported](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/is-supported)
- [initialize](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/initialize) · [release](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/release)
- [getLastKnownLocation](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/get-last-known-location)
- [registerFusedLocationCallback](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/register-fused-location-callback) · [unregisterFusedLocationCallback](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/unregister-fused-location-callback)
- [addProximityAlert](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/add-proximity-alert) · [removeProximityAlert](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/remove-proximity-alert)
- [showLocationAccessBlockedPopup](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/fused-location/ai.pleos.playground.location.fused/fused-location/show-location-access-blocked-popup)

## 구현·검증 체크리스트

- [ ] 위치를 쓰는 이유, 화면과 데이터 처리 범위를 정하고 사용자에게 설명했다.
- [ ] 지원 여부, 런타임 권한 허용·거부 및 차량의 위치 접근 차단 상태를 처리했다.
- [ ] `null`과 오류에 대응하고 등록한 리스너와 SDK를 각각 해제했다.
- [ ] 에뮬레이터와 대상 차량에서 필요한 기능·권한·정확도·생명주기를 검증했다.

[앱 개발 가이드](../pleos-development/README.md) · [NaviHelper 안내](../pleos-navihelper/README.md)
