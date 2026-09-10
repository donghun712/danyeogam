package com.danyeogam.backend.tourapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Component
public class TourApiClient {

    private static final String SUCCESS_CODE = "0000";
    private static final Map<String, String> LEGACY_TO_LEGAL_PROVINCE = Map.ofEntries(
            Map.entry("1", "11"),
            Map.entry("2", "28"),
            Map.entry("3", "30"),
            Map.entry("4", "27"),
            Map.entry("5", "12"),
            Map.entry("6", "26"),
            Map.entry("7", "31"),
            Map.entry("8", "36"),
            Map.entry("31", "41"),
            Map.entry("32", "51"),
            Map.entry("33", "43"),
            Map.entry("34", "44"),
            Map.entry("35", "47"),
            Map.entry("36", "48"),
            Map.entry("37", "52"),
            Map.entry("38", "12"),
            Map.entry("39", "50")
    );

    private final RestClient restClient;
    private final TourApiProperties properties;

    public TourApiClient(
            @Qualifier("tourApiRestClient") RestClient tourApiRestClient,
            TourApiProperties properties
    ) {
        this.restClient = tourApiRestClient;
        this.properties = properties;
    }

    public TourApiPage<TouristSummary> getAreaBasedList(int pageNo, int numOfRows, String areaCode) {
        return getAreaBasedList(pageNo, numOfRows, areaCode, null);
    }

    public TourApiPage<TouristSummary> getAreaBasedList(
            int pageNo,
            int numOfRows,
            String areaCode,
            String contentTypeId
    ) {
        return getAreaBasedList(
                pageNo, numOfRows, areaCode, contentTypeId, null, null, null
        );
    }

    public TourApiPage<TouristSummary> getAreaBasedList(
            int pageNo,
            int numOfRows,
            String areaCode,
            String contentTypeId,
            String classificationLevel1,
            String classificationLevel2,
            String classificationLevel3
    ) {
        validatePage(pageNo, numOfRows);
        JsonNode body = get("/areaBasedList2", uriBuilder -> {
            UriBuilder builder = commonQuery(uriBuilder, pageNo, numOfRows)
                    .queryParam("arrange", "C");
            if (StringUtils.hasText(areaCode)) {
                builder.queryParam("areaCode", areaCode);
            }
            if (StringUtils.hasText(contentTypeId)) {
                builder.queryParam("contentTypeId", contentTypeId);
            }
            if (StringUtils.hasText(classificationLevel1)) {
                builder.queryParam("lclsSystm1", classificationLevel1);
            }
            if (StringUtils.hasText(classificationLevel2)) {
                builder.queryParam("lclsSystm2", classificationLevel2);
            }
            if (StringUtils.hasText(classificationLevel3)) {
                builder.queryParam("lclsSystm3", classificationLevel3);
            }
            return builder.build();
        });

        return toPage(body, item -> new TouristSummary(
                text(item, "contentid"),
                text(item, "contenttypeid"),
                text(item, "title"),
                text(item, "addr1"),
                text(item, "addr2"),
                legalProvinceCode(item),
                legalDistrictCode(item),
                text(item, "mapx"),
                text(item, "mapy"),
                text(item, "firstimage"),
                text(item, "firstimage2"),
                text(item, "modifiedtime"),
                text(item, "tel"),
                text(item, "cpyrhtDivCd"),
                text(item, "lclsSystm1"),
                text(item, "lclsSystm2"),
                text(item, "lclsSystm3"),
                item.deepCopy()
        ));
    }

    public TourApiPage<TourRegionCode> getAreaCodes(
            String parentAreaCode,
            int pageNo,
            int numOfRows
    ) {
        validatePage(pageNo, numOfRows);
        JsonNode body = get("/areaCode2", uriBuilder -> {
            UriBuilder builder = commonQuery(uriBuilder, pageNo, numOfRows);
            if (StringUtils.hasText(parentAreaCode)) {
                builder.queryParam("areaCode", parentAreaCode);
            }
            return builder.build();
        });

        return toPage(body, item -> new TourRegionCode(
                text(item, "code"),
                text(item, "name")
        ));
    }

    public TourApiPage<TourRegionCode> getLegalDongCodes(
            String parentLegalProvinceCode,
            int pageNo,
            int numOfRows
    ) {
        validatePage(pageNo, numOfRows);
        JsonNode body = get("/ldongCode2", uriBuilder -> {
            UriBuilder builder = commonQuery(uriBuilder, pageNo, numOfRows);
            if (StringUtils.hasText(parentLegalProvinceCode)) {
                builder.queryParam("lDongRegnCd", parentLegalProvinceCode);
            }
            return builder.build();
        });

        return toPage(body, item -> new TourRegionCode(
                normalizedLegalDongCode(text(item, "code"), parentLegalProvinceCode),
                text(item, "name")
        ));
    }

    public Optional<TouristDetail> getCommonDetail(String contentId) {
        if (!StringUtils.hasText(contentId)) {
            throw new IllegalArgumentException("contentId는 필수입니다.");
        }

        JsonNode body = get("/detailCommon2", uriBuilder -> commonQuery(uriBuilder, 1, 10)
                .queryParam("contentId", contentId)
                .build());

        return toPage(body, item -> new TouristDetail(
                text(item, "contentid"),
                text(item, "title"),
                text(item, "addr1"),
                text(item, "addr2"),
                text(item, "mapx"),
                text(item, "mapy"),
                text(item, "overview"),
                text(item, "homepage"),
                text(item, "tel"),
                text(item, "firstimage"),
                text(item, "firstimage2"),
                text(item, "modifiedtime"),
                text(item, "cpyrhtDivCd")
        )).items().stream().findFirst();
    }

    public TourApiPage<TouristImage> getImages(String contentId, int pageNo, int numOfRows) {
        if (!StringUtils.hasText(contentId)) {
            throw new IllegalArgumentException("contentId는 필수입니다.");
        }
        validatePage(pageNo, numOfRows);

        JsonNode body = get("/detailImage2", uriBuilder -> commonQuery(uriBuilder, pageNo, numOfRows)
                .queryParam("contentId", contentId)
                .build());

        return toPage(body, item -> new TouristImage(
                text(item, "contentid"),
                text(item, "imgname"),
                text(item, "originimgurl"),
                text(item, "smallimageurl"),
                text(item, "serialnum"),
                text(item, "cpyrhtDivCd")
        ));
    }

    private JsonNode get(String path, Function<UriBuilder, java.net.URI> uriFunction) {
        if (!StringUtils.hasText(properties.getServiceKey())) {
            throw new TourApiException("CONFIGURATION_ERROR", "TourAPI 서비스 키가 설정되지 않았습니다.");
        }

        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriFunction.apply(uriBuilder.path(path)))
                    .retrieve()
                    .body(JsonNode.class);
            return responseBody(root);
        } catch (TourApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new TourApiException("TourAPI 호출에 실패했습니다.");
        }
    }

    private UriBuilder commonQuery(UriBuilder uriBuilder, int pageNo, int numOfRows) {
        return uriBuilder
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("MobileOS", properties.getMobileOs())
                .queryParam("MobileApp", properties.getMobileApp())
                .queryParam("_type", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows);
    }

    private JsonNode responseBody(JsonNode root) {
        if (root != null && root.has("resultCode")) {
            String resultCode = text(root, "resultCode");
            String resultMessage = text(root, "resultMsg");
            throw new TourApiException(resultCode, "TourAPI 오류: " + resultMessage);
        }
        if (root == null || !root.has("response")) {
            throw new TourApiException("INVALID_RESPONSE", "TourAPI 응답 형식이 올바르지 않습니다.");
        }
        JsonNode response = root.path("response");
        String resultCode = text(response.path("header"), "resultCode");
        if (!SUCCESS_CODE.equals(resultCode)) {
            String resultMessage = text(response.path("header"), "resultMsg");
            throw new TourApiException(resultCode, "TourAPI 오류: " + resultMessage);
        }
        return response.path("body");
    }

    private <T> TourApiPage<T> toPage(JsonNode body, Function<JsonNode, T> mapper) {
        List<T> result = new ArrayList<>();
        JsonNode itemNode = body.path("items").path("item");
        if (itemNode.isArray()) {
            itemNode.forEach(item -> result.add(mapper.apply(item)));
        } else if (itemNode.isObject()) {
            result.add(mapper.apply(itemNode));
        }

        return new TourApiPage<>(
                body.path("totalCount").asInt(0),
                body.path("pageNo").asInt(0),
                body.path("numOfRows").asInt(0),
                result
        );
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static String legalProvinceCode(JsonNode item) {
        String legalCode = text(item, "lDongRegnCd");
        if (StringUtils.hasText(legalCode)) {
            return legalCode.length() > 2 ? legalCode.substring(0, 2) : legalCode;
        }
        return LEGACY_TO_LEGAL_PROVINCE.get(text(item, "areacode"));
    }

    private static String legalDistrictCode(JsonNode item) {
        String legalProvinceCode = text(item, "lDongRegnCd");
        if (!StringUtils.hasText(legalProvinceCode) || legalProvinceCode.length() > 2) {
            return null;
        }
        return text(item, "lDongSignguCd");
    }

    private static String normalizedLegalDongCode(String code, String parentLegalProvinceCode) {
        if (!StringUtils.hasText(parentLegalProvinceCode) && StringUtils.hasText(code) && code.length() > 2) {
            return code.substring(0, 2);
        }
        return code;
    }

    private static void validatePage(int pageNo, int numOfRows) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo는 1 이상이어야 합니다.");
        }
        if (numOfRows < 1 || numOfRows > 1000) {
            throw new IllegalArgumentException("numOfRows는 1 이상 1000 이하여야 합니다.");
        }
    }
}
