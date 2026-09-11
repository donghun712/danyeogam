# Codex에게 붙여넣을 프롬프트 — 도감 시/군/구 단위 진행률

다녀감 백엔드(danyeogam) 저장소에서 작업을 진행해줘.

## 배경

현재 `GET /api/v1/me/collection/summary`는 광역 단위(도/특별시/광역시, 16개) 진행률만
내려줘. 도감 화면에서 광역 지역을 고른 다음, 그 안의 시/군/구별로도 방문 개수를 나눠서
보여주고 싶어.

## 확인해줘 — regionCode 작업 때와 같은 맥락

`Region` 엔티티에 이미 `parent` 관계와 `RegionLevel`(PROVINCE/CITY_COUNTY)이 있는 걸로
알고 있어(이전 regionCode 작업 때 확인했던 구조). 시/군/구 단위 관광지가 이미 CITY_COUNTY
레벨 `Region`에 연결되어 있다면, 집계 자체는 어렵지 않을 것 같은데 — 실제로 그런지 먼저
확인해줘.

## 제안하는 API 형태 (검토 후 조정 가능)

기존 엔드포인트를 그대로 확장하는 방식을 제안해:

```
GET /api/v1/me/collection/summary?parentRegionCode=TOUR:AREA:45
```

- `parentRegionCode` 없이 호출(기존 방식)하면 지금처럼 광역 단위 16개를 그대로 반환
  (기존 프론트 코드가 이미 이 방식으로 쓰고 있으니 하위 호환 꼭 유지해줘)
- `parentRegionCode`에 광역 코드를 넣으면, 그 광역에 속한 시/군/구 단위 `regions[]`를
  반환 — 응답 스키마(`code`, `name`, `visitedCount`, `totalCount`, `progressPercent`)는
  기존과 동일하게, `code`만 CITY_COUNTY 레벨 코드로.

다른 방식(별도 엔드포인트 등)이 프로젝트 구조상 더 자연스러우면 그렇게 해도 괜찮아 —
다만 기존 `GET /me/collection/summary`(파라미터 없는 호출)의 응답이 지금과 달라지면
안 돼(프론트가 이미 그 형태로 쓰고 있어서 깨지면 안 됨).

## 조건

- 기존 `GET /me/collection?regionCode=...`(관광지 목록 조회)는 광역 코드든 시/군/구
  코드든 상관없이 지금처럼 그대로 동작해야 해 — 이미 정확한 `regionCode`로 필터링하는
  구조니까 아마 손 안 대도 될 것 같은데, 확인해줘.
- 관광지 상세(`GET /tourist-spots/{id}`)의 `regionCode`는 지금처럼 PROVINCE 레벨로
  정규화해서 유지해줘(이건 바꾸지 마 — 도감 진행률 매칭에 이미 쓰이고 있어).
- Swagger·`docs/frontend-collection-api.md` 갱신해줘.

## 확인해야 할 것

1. 기존 테스트가 깨지지 않는지
2. `parentRegionCode` 없이 호출했을 때 기존과 완전히 동일한 응답이 나오는지 (회귀 확인)
3. 실제 광역 지역 하나(예: 전북특별자치도) 기준으로 시/군/구 단위 응답이 올바르게
   나오는지, 합계가 광역 단위 숫자와 맞는지

## 보고 형식

1. `Region` 계층 구조 실제 확인 결과
2. 변경한 파일 목록
3. 최종 API 스펙
4. 테스트 결과 (특히 기존 호출 방식 회귀 테스트)
5. 실제 응답 예시 (광역 단위 / 특정 광역의 시·군·구 단위 둘 다)

프론트엔드 코드는 건드리지 마 (별도 저장소). API 구조가 예상과 다르게 나오거나 애매한
부분이 있으면 임의로 넘기지 말고 먼저 알려줘.
