# Pleos Connect 앱 설계 원칙과 UI 디자인 가이드

이 문서는 이 프로젝트에서 차량용 앱을 기획하고 구현할 때 참고할 수 있도록 Pleos 공개 문서의 주제를 정리한 것입니다. 확인일: 2026-10-29. 정책은 차량, 플랫폼 버전, 지역 및 앱 카테고리에 따라 달라질 수 있으므로 구현과 심사 전 최신 원문을 다시 확인합니다.

- [앱 설계 원칙 전체 목차](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro)
- [UI 디자인 가이드 전체 목차](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)

이 문서는 원문을 대체하는 인증 기준이 아닙니다. 원문의 도표, 색상 토큰, 이미지로 제공되는 크기와 세부 상태는 아래 항목별 링크에서 직접 확인합니다. 이 문서에 항목을 정리했다고 해서 현재 앱에 해당 기능이 구현됐거나 심사 기준을 충족한다는 뜻은 아닙니다.

## 1. 차량 환경과 주행 안전

### 주의 분산 방지

- 운전 중 필요한 정보부터 짧고 명확하게 보여 주고, 자주 사용하는 기능을 접근하기 쉬운 위치에 둡니다. 메뉴 단계, 터치 횟수와 키보드 입력을 줄이고 최근 항목, 즐겨찾기, 프리셋 또는 지원되는 음성 입력을 활용합니다.
- 주행 중 시선을 붙잡는 영상, 자동 스크롤, 반복 화면 전환 및 장식용 움직임을 제한합니다. 영상은 주행 제한 상태에서 시작하거나 재개하지 않도록 합니다.
- 한 손으로 조작할 수 있도록 충분한 터치 영역과 간격, 명확한 피드백을 제공합니다. HUD, 클러스터, OSD를 사용하는 경우 해당 플랫폼 정책도 확인합니다.
- 내비게이션 안내 등 우선순위가 높은 소리가 재생되면 오디오 포커스 변화에 맞춰 다른 소리를 일시 정지하거나 조절합니다.
- 앱은 `CarUxRestrictions`로 현재 제한 상태를 확인하고, 제한되는 UI와 기능을 비활성화하거나 줄여야 합니다. 에뮬레이터에서 정상 작동한 것만으로 주행 안전 준수를 판정할 수는 없습니다.

원문: [주행 안전을 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)

### 주행 상태와 지역별 제한

규제는 속도, 기어, 브레이크 등 차량 상태와 지역 정책에 영향을 받습니다. Distraction Optimization을 선언하지 않은 앱은 문서상 주차 중 사용 가능, 주행 중 사용 불가로 구분됩니다. 실제 제한은 런타임의 `CarUxRestrictions`와 차량별 정책을 따릅니다.

아래는 원문에 제시된 **주행 중 예시**입니다. 검색은 차량 인포테인먼트 시스템의 통합 검색을 뜻하며 웹 검색과 구별합니다.

| 구분 | 북미 | 유럽 | 그 외 지역 |
| --- | --- | --- | --- |
| 키보드·다이얼패드 | 제한 | 허용 예시 | 허용 예시 |
| 통합 검색 | 음성으로만 허용 예시 | 허용 예시 | 허용 예시 |
| 미디어 로그인·설정·검색 | 제한 | 제한 | 허용 예시 |
| 차량 설정, 동영상, QR 로그인, 긴 매뉴얼 | 제한 | 제한 | 제한 |
| 내비게이션 검색 | 제한 | 허용 예시 | 허용 예시 |

주차 중에는 위 표에 열거된 기능이 허용되는 것으로 안내되지만, 앱 카테고리·차량 및 지역의 실제 조건을 함께 확인합니다. 제한 플래그에는 키보드, 다이얼패드, 필터링, 설정, 영상, 콘텐츠 양과 문자열 길이 관련 항목이 있습니다. InfoLINK의 지역별 키보드 분기는 원문 안내에 따라 별도 확인이 필요할 수 있습니다.

원문: [지역별 주행 규제 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)

### 앱 카테고리

기획 단계에서 앱의 목적을 Maps/Navigation, Mobility Services, Lifestyle, Entertainment, Business, Music/Audio, Kids, Social/Communication 중 적합한 카테고리로 정의합니다.

- Music/Audio는 간단한 재생 제어와 청각 중심 콘텐츠가 제한적으로 허용될 수 있습니다.
- Maps/Navigation은 간결한 지도, 경로·교통 정보 및 음성 안내가 제한적으로 허용될 수 있습니다.
- Mobility Services는 위치 기반 충전소·주유소·주차장 정보 등 주행과 관련된 기능이 제한적으로 허용될 수 있습니다.

이는 허용 가능성의 예시이며 모든 지역이나 주행 상태에서의 허용을 보장하지 않습니다. 원문: [앱 카테고리](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category)

## 2. 앱 구조와 플랫폼 연동

### 화면과 생명주기

- 차량 정보가 표시되는 Driving view는 시스템 영역입니다. 앱이 임의로 가리거나 종료하는 영역으로 설계하지 않습니다.
- 앱 영역은 차량 화면 구성에 따라 한 개 또는 두 개의 창으로 표시됩니다. 좁은 portrait 화면에는 핵심 정보와 기능을 우선 배치하고, 넓은 landscape 화면에는 추가 정보를 배치합니다.
- 사용자가 창 크기나 위치를 바꾸거나 다른 앱을 열 수 있으므로 화면 크기와 방향 변경에 대응합니다. 시스템 UI 패널과 키보드가 앱 화면을 일시적으로 가릴 수 있습니다.
- 화면에서 사라진 앱이 바로 종료된다고 가정하지 않습니다. 포그라운드 복귀 시 상태를 복원하고, 백그라운드에서는 불필요한 화면 갱신, 센서 수집, 타이머와 요청을 줄입니다. 계속 실행할 재생·통신·다운로드 작업은 중단과 재개 조건을 정합니다.
- 문서는 별도의 `screenOrientation` 지정이 없는 Activity의 기본 창과 `landscape` 지정 시의 창 사용을 설명합니다. 실제 차량·디스플레이 및 창 전환 동작은 원문을 확인합니다.

원문: [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model), [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window)

### 하드웨어와 네트워크

- 차량 하드웨어에 직접 접근하지 않고, 지원되는 플랫폼 API와 Pleos Connect SDK를 사용합니다. 전화, 문자, 일부 센서, 캘린더 및 주소록 등 모바일 API는 차량에서 지원 여부가 다를 수 있습니다.
- 이동 중 통신 품질 저하와 단절을 고려하여 최근 콘텐츠 캐시, 적절한 타임아웃과 재시도, 오프라인 상태 안내를 설계합니다.

원문: [하드웨어 접근 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture)

### API 선택과 가용성

- 동일하거나 비슷한 기능이 AAOS 표준에 있는 경우 표준 API 사용을 우선 검토합니다. Pleos SDK는 표준으로 접근하기 어렵거나 제조사 특화 기능이 필요한 영역을 제공합니다.
- Pleos SDK 전용 기능이 AAOS 표준으로 수용되는 경우, 문서는 기존 SDK API에 최소 두 개의 주요 버전까지 하위 호환성을 유지하는 정책을 설명합니다. 변경 사항은 릴리스 노트에서 확인합니다.
- Vehicle SDK 기능 지원은 차량 모델별로 다를 수 있습니다. `checkXXXCapabilities`로 현재 대상 차량의 지원 범위를 확인하고, 지원하지 않는 경우를 UI와 동작에 반영합니다. FusedLocation은 `isSupported`로 확인합니다.
- AAOS 차량 프로퍼티도 실제 지원·접근 가능 여부를 확인합니다. 모델명만으로 기능을 추측하지 않으며, 특수 프로퍼티를 직접 구현하기 전에는 관련 기술 지원 절차를 확인합니다.
- 내비게이션 SDK, 음성 SDK 등의 호환성은 차량 구성과 문서에 명시된 조건을 확인합니다.

원문: [SDK API 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api), [API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)

### 보안과 개인정보

- 차량을 여러 사람이 이용할 수 있으므로 운전자 교체 또는 소유자 변경 시 로그아웃과 계정 전환이 가능하게 합니다. 인증 토큰은 Keystore와 같은 안전한 저장소에 보관합니다.
- 위치·음성·계정 정보가 필요한 이유를 알기 쉽게 설명하고 필요한 정보만 수집합니다. 로그에 개인정보가 포함되지 않도록 하거나 마스킹합니다.
- 차량 또는 사용자 식별자의 익명화·가명화 방침을 정하고, 주행 행동 데이터의 마케팅 목적 사용을 구분합니다. 배포 전 별도의 앱 보안 및 개인정보 기준을 확인합니다.

원문: [보안 및 개인정보 보호를 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy)

### 성능과 리소스

초기 화면에 필요한 항목부터 로딩하고 나머지는 지연 처리합니다. 백그라운드 작업과 폴링을 줄이고 목록·이미지 캐시의 메모리 사용을 관리합니다. 오디오 앱은 오디오 포커스의 획득과 상실에 대응해야 합니다.

문서에 제시된 리소스 기준은 다음과 같습니다. 측정 조건과 최신 심사 기준은 원문에서 확인합니다.

| 상황 | CPU 사용 기준 |
| --- | --- |
| 내비게이션 앱 주행 중 | 15% 이하 |
| 그 외 앱 주행 중 | 5% 미만 |
| 모든 앱 정차 중 | 25% 이하 |

RAM은 400 MB 이하로 유지하도록 안내합니다. 원문: [성능 및 리소스 효율을 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt)

## 3. UI 디자인 공통 원칙

- **쉽고 안전한 사용:** 시각적·인지적 부담과 조작을 줄이고 한 손 사용을 고려합니다.
- **일관성:** 용어, 아이콘, 상호작용을 플랫폼의 디자인 패턴과 맞춥니다.
- **한눈에 읽기:** 핵심 정보를 빠르게 파악할 수 있게 합니다. 안내 문서는 정보 파악 시간 1~2초, 입력 후 응답 0.25초 이내를 제시하며 시간이 더 걸릴 때는 진행 상태를 알리도록 합니다.
- **대비와 가독성:** 텍스트·아이콘·이미지에 배경 대비 4.5:1 이상을 안내합니다. 중요한 기본 텍스트는 물리적 높이 5 mm 이상, 보조 텍스트는 3 mm 이상을 제시합니다. 실제 화면 및 차량에서 검증합니다.
- **문장 길이:** 긴 문장을 피하고, 영문 단일 메시지는 120자 또는 24단어 이내, 한 줄은 영문 기준 80자 이내, 문장·단락은 세 줄 이내 기준을 확인합니다. 언어별 기준과 원문의 표기에는 세부 조건이 있으므로 원문을 따릅니다.
- **아이콘과 피드백:** 의미를 쉽게 이해할 수 있는 아이콘과 필요한 텍스트를 사용하고 터치에 대한 적절한 시각·촉각·음성 피드백을 고려합니다.
- **알림:** 운전자에게 필요한 내용만 간결하게 표시하고 광고 관련 제한을 확인합니다.
- **확장:** 주간·야간 환경, 다국어, 화면 크기와 비율 변화, 음성 인터랙션 및 업데이트를 고려합니다.

원문: [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)

## 4. 화면 구성과 디자인 기초

### Getting Started

| 항목 | 적용 시 확인할 내용 | 원문 |
| --- | --- | --- |
| Display Layout | 상태 표시줄, Driving view, GNB와 앱 창의 역할 및 시스템 레이어를 구분합니다. Connect-S, Connect-W, W-Extended, WF 등 화면 형태를 확인합니다. | [Display Layout](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) |
| Status bar | 주행 및 시스템 상태와 알림·프로필 등 시스템 표시 영역을 이해합니다. | [Status bar](https://document.pleos.ai/docs/connect/guide/docs-design/components/status-bar) |
| Global Navigation Bar | 앱 실행, 최근 앱, 차량 설정과 공조 등 시스템 내비게이션 영역을 고려합니다. | [GNB](https://document.pleos.ai/docs/connect/guide/docs-design/components/gnb) |
| App library | 설치된 앱, 공조 제어, 편집과 페이지 이동이 제공되는 시스템 영역을 확인합니다. | [App library](https://document.pleos.ai/docs/connect/guide/docs-design/components/app-library) |
| Window Policy | Connect-S와 Connect-W의 창 비율과 앱 실행·전환 동작을 확인합니다. | [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) |

### Foundations

| 항목 | 적용 시 확인할 내용 | 원문 |
| --- | --- | --- |
| Colors | 기본 색상, 투명 색상, 테마와 무관한 정적 색상 및 용도별 시스템 토큰을 구분합니다. 실제 토큰 값과 시각 예시는 원문 이미지를 확인합니다. | [Colors](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors) |
| Typography | 정보의 중요도에 따라 글자 크기와 보조 텍스트의 위계를 일정하게 적용합니다. 원문은 12sp 이상 권장과 시인성 관련 예시를 제시합니다. | [Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography) |
| Iconography | 직관적인 아이콘을 사용하고 기본 48×48dp 그리드, 채움·윤곽 스타일과 크기 변형을 확인합니다. | [Iconography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/contents-creation/iconography) |
| App Icon | 전경과 배경을 분리한 Android Adaptive Icon 구조와 차량 브랜드별 마스킹을 확인합니다. | [App Icon](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/contents-creation/app-icon) |
| Animation | 로딩과 상태 설명에 필요한 움직임을 사용하고 주행 중 시선 분산을 피합니다. | [Animation](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation) |
| Shadow | UI 요소의 높이와 계층에 맞는 그림자 수준 및 수치를 확인합니다. | [Shadow](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/shadow) |

공식 디자인 리소스와 화면 가이드는 [UI 디자인 가이드의 Design Resources Download](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)에서 제공됩니다. 색상표, 아이콘 그리드, 그림자, 크기와 간격의 도식은 원본 자료를 기준으로 작업합니다.

## 5. 컴포넌트 목록

각 컴포넌트의 정확한 크기, 여백, 상태별 시각 표현과 동작은 원문 도식과 디자인 리소스를 확인합니다. 아래 링크는 디자인 가이드에 표시된 모든 컴포넌트 분야와 하위 항목을 찾아갈 수 있는 목록입니다.

| 분야 | 사용 목적과 확인 사항 | 원문 |
| --- | --- | --- |
| Actions | 기본 동작, 켜기·끄기, 텍스트 링크, 아이콘 동작 및 키 입력. 누름·비활성·로딩 등의 상태를 구분합니다. | [Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button) · [Toggle button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/toggle-button) · [Text button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/text-button) · [Icon button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/icon-button) · [Keypad](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/keypad) |
| Bar | 창 크기와 콘텐츠 조작에 쓰는 바와 핸들의 상태를 확인합니다. | [Bar](https://document.pleos.ai/docs/connect/guide/docs-design/components/bar) |
| Controllers | 최솟값·최댓값·단계가 있는 값을 슬라이더나 클릭으로 조절합니다. | [Controllers](https://document.pleos.ai/docs/connect/guide/docs-design/components/controllers) |
| Indicator | 현재 선택 값과 인접한 선택 값을 표시합니다. | [Indicator](https://document.pleos.ai/docs/connect/guide/docs-design/components/indicator) |
| Dropdown | 메뉴를 열고 닫는 상태, 선택 및 비활성 상태를 표시합니다. | [Dropdown](https://document.pleos.ai/docs/connect/guide/docs-design/components/dropdown) |
| Fields | 문자 또는 숫자 입력에서 포커스, 읽기 전용, 비활성, 유효성 상태를 처리합니다. 주행 제한에 따른 입력 차단도 고려합니다. | [Text Field](https://document.pleos.ai/docs/connect/guide/docs-design/components/fields/text-field) · [Number Field](https://document.pleos.ai/docs/connect/guide/docs-design/components/fields/number-field) |
| Page Navigation | 앱의 주요 페이지, 뒤로 가기 및 필요한 메뉴를 배치합니다. | [Page Navigation](https://document.pleos.ai/docs/connect/guide/docs-design/components/page-navigation) |
| Popups | 제목, 설명, 확인과 취소 등으로 필요한 결정을 전달하거나 일시적인 메시지를 표시합니다. | [System Popup](https://document.pleos.ai/docs/connect/guide/docs-design/components/popups/system-popup) · [Toast Popup](https://document.pleos.ai/docs/connect/guide/docs-design/components/popups/toast-popup) |
| Tabs | 두 개 이상의 콘텐츠나 기능 범주를 전환하고 현재 선택을 표시합니다. | [Tabs](https://document.pleos.ai/docs/connect/guide/docs-design/components/segmented-menu) |
| Selections | 다중 선택은 체크박스, 단일 선택은 라디오 버튼, 켜기·끄기는 스위치로 표현합니다. | [Checkbox](https://document.pleos.ai/docs/connect/guide/docs-design/components/selections/checkbox) · [Radio button](https://document.pleos.ai/docs/connect/guide/docs-design/components/selections/radio) · [Switch](https://document.pleos.ai/docs/connect/guide/docs-design/components/selections/switches) |
| Spinner | 날짜·시간 등의 선택 가능한 값을 탐색하고 현재 값을 표시합니다. | [Spinner](https://document.pleos.ai/docs/connect/guide/docs-design/components/spinner) |
| System Notification | 시스템 알림의 아이콘·내용·동작과 맥락을 확인합니다. | [System Notification](https://document.pleos.ai/docs/connect/guide/docs-design/components/notification/system-notification) |
| Toast | 짧은 알림을 보여 주며 화면 안쪽 간격과 최대 폭에 관한 원문 규칙을 확인합니다. | [Toast](https://document.pleos.ai/docs/connect/guide/docs-design/components/toast) |
| Widgets | 위젯 제목, 정보, 조작, 페이지 표시 및 숨김 동작을 확인합니다. | [Widgets](https://document.pleos.ai/docs/connect/guide/docs-design/components/widgets) |

## 6. 이 프로젝트에 적용할 때

- 현재 프로젝트에는 `mobile`, `automotive`, `shared` 모듈이 있습니다. 실제 차량에 배포할 앱의 실행 모듈과 화면 구성을 정한 후 관련 가이드를 적용합니다.
- 현재 `mobile` 모듈의 첫 화면은 Vehicle SDK로 왼쪽 앞문 위치를 읽는 예제입니다. 조회 성공 외에도 서비스 연결 실패, `null`, 권한 및 차량 기능 미지원 상태를 사용자에게 이해하기 쉽게 표시하도록 발전시킵니다.
- 도어 위치 조회는 `CAR_DOORS` 권한이 선언되어 있습니다. 실제 차량의 지원 여부는 capability API와 해당 차량 환경에서 확인합니다.
- 주행 제한 처리, 다양한 창 크기, 주간·야간 테마, 접근성, 개인정보 및 리소스 기준은 별도 구현과 검증이 필요합니다. 이 README는 그러한 기능을 구현하거나 준수 여부를 인증하지 않습니다.

## 7. 검토 체크리스트

- [ ] 앱 카테고리와 제공 지역을 정하고 해당 지역·차량·플랫폼의 주행 제한을 확인했다.
- [ ] `CarUxRestrictions` 변화에 따라 제한 기능과 영상·키보드 동작을 처리했다.
- [ ] portrait/landscape, 단일·분할 창, 백그라운드 전환과 상태 복원을 확인했다.
- [ ] 필요한 차량 API의 권한, capability, 오류와 미지원 상태를 처리했다.
- [ ] 주야간 대비, 글자 크기, 콘텐츠 길이, 터치 영역, 다국어와 피드백을 검토했다.
- [ ] 계정 전환, 안전한 토큰 저장, 개인정보 최소화 및 로그 처리를 검토했다.
- [ ] 오프라인 상태와 재시도, 시작 시간, CPU·RAM 및 오디오 포커스를 검토했다.
- [ ] 원문 이미지와 디자인 리소스의 최신 치수·색상·컴포넌트 상태 및 앱 심사 기준을 확인했다.
