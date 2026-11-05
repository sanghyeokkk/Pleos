# NaviHelper SDK 개발 안내

> 현재 차계부 구현: `requestNavigationScreen(SearchPlace("블루핸즈"))`로 Pleos 내비게이션의 장소 검색 화면을 요청합니다. `NAVI_CUSTOM_MAP` 권한을 선언했으며 에뮬레이터에서 지점 검색, 선택과 경로 안내 시작을 확인했습니다. 아래의 기존 구현 상태 설명은 이 기능 추가 전 작성된 부분이 있을 수 있습니다. 최신 사항은 [차계부 README](../car-care/README.md)를 참조하세요.

이 문서는 Pleos NaviHelper 공개 가이드의 내용을 이 프로젝트에 맞게 요약하고, 프로그래밍 가이드와 API 참조의 패키지·클래스·메서드 주소를 모은 목록입니다. 확인일: 2026-10-29. 제공되는 이벤트와 API 사용 조건은 차량, 내비게이션 앱, 서비스 및 SDK 버전에 따라 달라질 수 있으므로 실제 개발 시 원문을 확인합니다.

- [NaviHelper 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [프로그래밍 가이드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk)
- [API Reference 전체 목차](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/entry)
- [프로젝트 앱 개발 가이드](../pleos-development/README.md) · [앱 설계·UI 가이드](../pleos-design/README.md)

## 1. SDK 용도와 프로젝트 설정

NaviHelper는 Pleos Connect의 내비게이션 서비스와 연동해 목적지, 경로, 경유지, 안내 정보 및 Navigation 화면 요청 등을 처리합니다. 요청 결과와 상태 변화는 리스너 이벤트로 받습니다. 이 SDK는 Vehicle SDK와 별도로 앱 모듈에 추가합니다.

- SDK 버전: `ai.pleos.playground:NaviHelper:3.0.5`
- 적용 모듈: `mobile/build.gradle.kts`
- Maven 저장소: `settings.gradle.kts`에 이미 등록된 `https://nexus-playground.pleos.ai/repository/maven-releases/`
- 현재 적용 상태: `mobile` 앱에서 초기 서비스 연결을 시도하고 연결 성공 시 오류 이벤트 리스너를 등록합니다. 화면에 최초 연결 결과를 표시하고 Activity가 종료되면 리스너를 제거하고 SDK를 해제합니다. 경로·목적지의 조회나 변경 기능은 아직 구현하지 않았습니다.

원문: [SDK 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk)

## 2. 권한

문서는 기능별로 다음 권한을 안내합니다. 개별 API의 최신 `@RequiresPermission` 명세를 확인하고, 앱에서 실제 사용하는 기능에 필요한 권한만 매니페스트에 선언합니다.

| 권한 | 문서에 제시된 용도 예시 |
| --- | --- |
| `pleos.car.permission.NAVI_ROUTE` | 정보 조회, `requestTrip()` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 경로 요청·취소·재탐색, 경유지와 경로 옵션 변경 |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | Navigation 화면 요청 |

현재 `mobile` 앱에는 이후 목적지·경유지 정보 조회와 경로 요청 기능을 개발할 수 있도록 `NAVI_ROUTE`와 `NAVI_ROUTE_SEARCH`를 선언했습니다. 실제 경로 API 호출은 아직 구현하지 않았습니다. 지도 화면 요청용 `NAVI_CUSTOM_MAP`은 해당 기능을 구현할 때 API 명세와 함께 검토합니다. 앱 배포 시 선언한 권한이 실제 기능과 승인 범위에 부합하는지 확인하고, 사용하지 않는 권한은 제거합니다.

원문: [권한 선언](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)

## 3. 연결과 이벤트

1. `NaviHelper` 객체를 생성하고 `initialize(onConnected, onFailed)`로 최초 연결 결과를 확인합니다.
2. 연결 후 `NaviHelperEventListener`를 등록하고 필요한 요청이나 조회 API를 호출합니다.
3. 경로 탐색 시작·완료, 목적지·경유지 변경, 정보 조회와 오류를 해당 이벤트로 처리합니다.
4. 사용이 끝나면 같은 리스너를 제거하고 SDK를 `release()`합니다. 앱 생명주기와 서비스 연결 상태를 함께 고려합니다.

`requestRoute()`가 오류 없이 반환된 것만으로 경로 탐색이 완료된 것은 아닙니다. 정보 조회도 대응되는 이벤트에서 결과를 확인합니다. 초기 연결 실패는 초기화의 `onFailed`, 내비게이션 요청 처리 오류는 리스너의 `onError`로 전달됩니다.

현재 앱의 `onConnected`는 최초 연결 성공과 리스너 등록을 나타냅니다. 이후 연결이 계속 유지된다는 보장이나 경로 조회 성공을 나타내지는 않습니다. Activity 종료 후 늦게 도착하는 연결 콜백은 화면에 반영하지 않습니다.

원문: [초기화와 해제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release), [이벤트 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners), [오류 처리](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling)

## 4. 목적지·경로·경유지

- `requestRoute()`로 목적지를 요청하고 `getDestinationInfo()` 결과는 이벤트로 받습니다. `cancelRoute()`는 경로를 취소합니다.
- 경로 옵션은 추천, 최단 거리·시간, 무료 도로, 고속도로, 편안한 경로, 스쿨존 고려 등 문서에 정의된 값을 사용합니다. 변경과 재탐색 결과도 이벤트로 확인합니다.
- 경유지는 문서 기준 최대 두 개이며 `WaypointIndex.FIRST`와 `SECOND`로 구분합니다. `addWaypoint()`, `getWaypointInfo()`, `removeWaypoint()`와 관련 이벤트를 확인합니다.
- `requestRouteWithWaypoints()`는 목적지와 경유지 목록을 하나의 요청으로 전달합니다. 인자·제약·권한은 API별 상세 명세를 확인합니다.
- 목적지와 경로를 변경하는 요청은 사용자 의도와 주행 제한을 반영해야 하므로 단순 설치 확인 과정에서 임의로 호출하지 않습니다.

### 요청과 결과 연결

| 작업 | 요청 | 결과·상태를 확인할 이벤트 |
| --- | --- | --- |
| 목적지 요청 | `requestRoute(RouteInfo)` | `onRouteStarted`, `onRoutingCompleted`, `onDestinationChanged`, `onError` |
| 목적지 정보 | `getDestinationInfo()` | `onDestinationInfo` |
| 경로 옵션 변경 | `changeRouteOption()` | `onRouteOptionChanged` |
| 경로 재탐색 | `requestReRoute()` | `onReRouteEnded` 등 실제 Navigation 앱이 전달하는 이벤트 |
| 경유지 조회 | `getWaypointInfo()` | `onWayPointInfo` |
| 경유지 포함 요청 | `requestRouteWithWaypoints(RouteWithWaypointsRequest)` | `onRouteStarted`, `onRoutingCompleted`, `onError` |

`RouteInfo`는 목적지 좌표와 POI 정보를 담고, `RequestWaypointInfo`는 경유지 좌표와 인덱스를 담습니다. 경로 옵션 변경이나 재탐색은 활성 경로가 있을 때 요청합니다. 경유지를 모두 제거할 때는 `WaypointIndex.ALL`을 사용합니다. 경유지 조회 리스너 이름은 실제 SDK의 `onWayPointInfo` 표기를 따릅니다.

### 오류 구분

초기 연결 오류는 `initialize`의 `onFailed`로, Navigation 앱의 요청 처리 오류는 리스너의 `onError`로 전달됩니다. 문서의 `NaviErrorCode` 예시는 배터리 부족(`CAUSE_LOW_BATTERY`), 탐색 가능 거리 초과(`CAUSE_SEARCH_ROUTE_OVER_DISTANCE`), 목적지가 너무 가까움(`CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`), 경로 탐색 서버 오류(`CAUSE_SEARCH_ROUTE`)입니다. 이외 코드도 처리해야 합니다. 오류 발생 시 서비스 연결, 권한, 좌표·POI·경유지 인덱스와 Navigation 앱 상태를 확인합니다.

현재 앱의 리스너는 위 네 가지 오류에 이해하기 쉬운 메시지를 표시하고 다른 코드는 코드 이름을 표시합니다. 아직 경로 요청과 결과 이벤트 처리는 구현하지 않았습니다.

원문: [목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations), [경로](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes), [경유지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings), [경유지 포함 경로 요청](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints)

## 5. API 패키지

| 패키지 | 용도 | 원문 |
| --- | --- | --- |
| `ai.pleos.playground.navi.constants` | 경로 옵션, 상태, 오류 및 구분 값 | [constants](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/entry) |
| `ai.pleos.playground.navi.data` | 목적지·경유지·경로·안내 및 주행 정보 모델 | [data](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/entry) |
| `ai.pleos.playground.navi.helper` | SDK 진입점과 요청·조회 메서드 | [helper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/entry) · [NaviHelper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/entry) |
| `ai.pleos.playground.navi.helper.listener` | 요청 결과 및 상태 변화 리스너 | [listener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper.listener/entry) · [NaviHelperEventListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper.listener/navi-helper-event-listener/entry) |

## 6. 테스트 환경

Pleos Connect 전용 에뮬레이터 및 Navigation 서비스가 필요하며, 실제 기능과 이벤트는 탑재된 Navigation 앱에 따라 달라질 수 있습니다. SDK가 빌드됐다는 것은 의존성 해결을 확인한 것이며 서비스 연결이나 경로 안내 성공을 의미하지 않습니다. SDK 연동 시 연결 성공·실패, 권한, 리스너 해제, 앱 재시작 및 주행 제한을 확인합니다. 음성 기능 테스트가 필요할 때는 PC의 마이크 장치와 에뮬레이터 마이크 설정을 별도로 확인합니다.

## 7. API 클래스·메서드 주소 목록

아래는 API 목차에서 확인한 클래스와 NaviHelper 메서드의 직접 링크입니다. 각 클래스 안의 세부 프로퍼티와 열거형 값은 해당 문서를 열어 확인합니다. † 표시된 주소는 목차에서 확인했지만 문서 내용을 가져오는 과정에서 오류 또는 시간 초과가 발생해 세부 내용을 검증하지 못했습니다.


### 상수 및 열거형

- [BookmarkType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/bookmark-type/entry)
- [CameraType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/camera-type/entry)
- [CautionPointType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/caution-point-type/entry)
- [NaviErrorCode](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navi-error-code/entry)
- [NavigationStatus](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navigation-status/entry)
- [NaviMode](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/navi-mode/entry)
- [RouteOption](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-option/entry) †
- [RoutePointPoiType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-poi-type/entry)
- [RoutePointSettingType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-setting-type/entry) †
- [RoutePointType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/route-point-type/entry)
- [TrafficCongestionType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/traffic-congestion-type/entry) †
- [TrafficEventType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/traffic-event-type/entry)
- [TurnType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/turn-type/entry)
- [WaypointCmdType](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/waypoint-cmd-type/entry) †
- [WaypointIndex](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.constants/waypoint-index/entry)

### 데이터 모델 (1)

- [BookmarkInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/bookmark-info/entry)
- [CameraAlert](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/camera-alert/entry)
- [CautionPoint](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/caution-point/entry)
- [ChargerOperator](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/charger-operator/entry)
- [ChargingPlan](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/charging-plan/entry)
- [CurrentLocationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/current-location-info/entry)
- [DestinationArrivedInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/destination-arrived-info/entry)
- [DestinationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/destination-info/entry)
- [DrivingInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/driving-info/entry)
- [DrivingState](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/driving-state/entry)
- [NavigationScreenRequest](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/navigation-screen-request/entry)
- [RecentDestinationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/recent-destination-info/entry)
- [RequestWaypointInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/request-waypoint-info/entry)
- [RouteInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-info/entry)
- [RouteOverviewShowInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-overview-show-info/entry)
- [RoutePointInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-point-info/entry) †

### 데이터 모델 (2)

- [RouteStartInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-start-info/entry)
- [RouteStateInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-state-info/entry)
- [RouteWithWaypointsRequest](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/route-with-waypoints-request/entry)
- [TBTInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/t-b-t-info/entry)
- [TrafficCongestionInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-congestion-info/entry)
- [TrafficCongestionSummary](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-congestion-summary/entry)
- [TrafficEventInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-event-info/entry)
- [TrafficInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-info/entry)
- [TrafficStatus](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/traffic-status/entry)
- [TripInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-info/entry)
- [TripRequest](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-request/entry)
- [TripSummary](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/trip-summary/entry) †
- [WaypointArrivedInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-arrived-info/entry)
- [WaypointChangedInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-changed-info/entry)
- [WaypointInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.data/waypoint-info/entry)

### NaviHelper 생성·기본 메서드

- [Constructor](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/-navi-helper)
- [addListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/add-listener)
- [addWaypoint](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/add-waypoint)
- [cancelRoute](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/cancel-route) †
- [changeRouteOption](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/change-route-option)
- [getBookmarkInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-bookmark-info)
- [getChargerOperatorInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-charger-operator-info)
- [getCurrentLocationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-current-location-info)
- [getDestinationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-destination-info)
- [getRecentDestinationInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-recent-destination-info)
- [getRouteStateInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-route-state-info)

### NaviHelper 조회·경로 메서드

- [getTBTInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-t-b-t-info)
- [getWaypointInfo](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/get-waypoint-info)
- [initialize](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/initialize)
- [release](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/release)
- [removeListener](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/remove-listener) †
- [removeWaypoint](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/remove-waypoint)
- [requestNavigationScreen](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/request-navigation-screen)
- [requestReRoute](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/request-re-route) †
- [requestRouteWithWaypoints](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/request-route-with-waypoints)
- [requestRoute](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/request-route)
- [requestTrip](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/navi-helper/ai.pleos.playground.navi.helper/navi-helper/request-trip)

## 8. 작업 전 체크리스트

- [ ] 사용할 기능과 사용자 동작을 정하고 해당 API의 권한을 선언했다.
- [ ] 초기 연결과 실패를 처리하고, 필요 시 리스너 등록·해제 및 SDK 해제를 구현했다.
- [ ] 경로·목적지 변경 결과를 이벤트로 확인하고, 실패와 미지원 상태를 처리했다.
- [ ] 주행 제한과 내비게이션 앱의 동작 조건을 확인했다.
- [ ] 에뮬레이터와 실제 대상 차량에서 서비스·권한·이벤트를 검증했다.

