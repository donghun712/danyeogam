# 백엔드 요청서 — 관광지 상세 응답에 regionCode 추가

## 배경

프론트엔드 "스탬프 획득!" 화면에 지역별 진행률(예: "전북 12/40")을 표시하려면, 관광지 상세 API가
내려주는 정보와 `GET /me/collection/summary`가 내려주는 지역별 진행률을 연결할 값이 필요합니다.

현재 `GET /api/v1/tourist-spots/{id}` 응답(`TouristSpotDetailResponse`)에는 지역 코드가 없어서
이 연결이 불가능한 상태입니다. `regionCode` 필드 하나를 추가해주시면 프론트에서 바로 연동
가능합니다.

## 요청 내용

**파일**: `backend/src/main/java/com/danyeogam/backend/touristspot/application/dto/TouristSpotDetailResponse.java`

```java
public record TouristSpotDetailResponse(
        long id,
        String name,
        String type,
        boolean stampEnabled,
        String visitState,
        TouristSpotAddressResponse address,
        PositionResponse position,
        String overview,
        List<TouristSpotImageResponse> images,
        String telephone,
        String homepageUrl,
        NavigationDestinationResponse navigation,
        String dataSource,
        String dataQuality,
        Instant lastSyncedAt,
        String regionCode   // ← 추가 요청
) { ... }
```

이 값을 만드는 서비스 코드(`TouristSpotDetailResponse`를 조립하는 곳, 아마
`touristspot/application` 패키지 안의 조회 서비스)에서 `TouristSpot.getRegion()`을 이용해
채워주시면 됩니다.

## ⚠️ 중요 — PROVINCE 레벨 코드로 올려서 반환해야 합니다

`TouristSpot.region`은 실제로는 **시/군/구(CITY_COUNTY) 레벨**로 연결되어 있는 경우가 많습니다.
`TourSyncPersistenceService.regionCode()`를 보면:

```java
static String regionCode(String areaCode, String districtCode) {
    if (districtCode == null || districtCode.isBlank()) {
        return provinceCode(areaCode);          // "TOUR:AREA:45"
    }
    return provinceCode(areaCode) + ":" + districtCode;  // "TOUR:AREA:45:1234"
}
```

시/군/구 정보가 있는 대부분의 관광지는 `"TOUR:AREA:45:1234"` 같은 **CITY_COUNTY 레벨** 코드로
연결됩니다. 반면 `GET /me/collection/summary`가 내려주는 `regions[].code`는 **PROVINCE 레벨**
코드(`"TOUR:AREA:45"`)만 사용합니다(16개 광역 지역 기준).

**따라서 `spot.getRegion().getCode()`를 그대로 반환하면 안 됩니다.** 그대로 반환하면 대부분의
관광지에서 코드가 일치하지 않아 프론트에서 진행률 매칭이 조용히 실패하게 됩니다(에러도 안 나고
그냥 진행률이 영원히 안 뜨는 상태가 됩니다).

**요청**: 아래처럼 region의 레벨을 확인해서 PROVINCE 레벨 코드로 정규화한 뒤 반환해주세요.

```java
private String resolveProvinceRegionCode(Region region) {
    Region current = region;
    while (current.getLevel() != RegionLevel.PROVINCE && current.getParent() != null) {
        current = current.getParent();
    }
    return current.getCode();
}
```

(정확한 위치·이름은 기존 코드 스타일에 맞춰 자유롭게 조정하셔도 됩니다. 핵심은 "항상
PROVINCE 레벨 코드가 나가야 한다"는 것입니다.)

## Swagger / 문서 갱신

- `backend/docs/openapi.yaml` (또는 자동 생성되는 경우 재생성)
- `backend/docs/frontend-read-api.md`의 관광지 상세 응답 예시에 `regionCode` 필드 추가

## 완료 기준 (Definition of Done)

- [ ] `TouristSpotDetailResponse`에 `regionCode: String` 필드 추가
- [ ] 값이 항상 PROVINCE 레벨 코드(`"TOUR:AREA:숫자"` 형태, 콜론 뒤에 추가 세그먼트 없음)로 반환됨
- [ ] `GET /me/collection/summary`가 반환하는 `regions[].code` 중 하나와 반드시 일치함 (실제 DB로 교차 확인 권장)
- [ ] Swagger/`openapi.yaml`에 반영
- [ ] `docs/frontend-read-api.md` 예시 갱신
- [ ] 기존 응답 필드는 변경 없음 (프론트가 이미 사용 중인 다른 필드들의 이름/타입 유지)

## 참고 — 이번 요청과 별개로 아직 팀 결정이 필요한 것 (지금 코드 작업 아님)

아래 두 가지는 이번 `regionCode` 작업과는 무관하지만, 조만간 팀 논의가 필요해서 같이 적어둡니다.
**지금 당장 코드로 구현해달라는 요청이 아니라, 정책 결정이 먼저 필요한 항목입니다.**

1. **STAMP_TARGET 자동 활성화 정책** — 현재 유효 좌표만 있으면 카테고리 무관하게 전부 스탬프 대상으로
   활성화되고 있습니다(`TourSyncPersistenceService`). 원래 기획 의도(선정 기준에 따른 큐레이션)와
   다른 상태라 팀 확인이 필요합니다.
2. **GPS 인증 반경/정확도/유효시간 운영값** — 현재 로컬 테스트값(100m/50m/2분)으로만 되어 있고
   실제 운영값은 미확정입니다. 서비스 오픈 전 현장 테스트 후 확정이 필요합니다.
