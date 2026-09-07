package com.danyeogam.backend.kakao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
public class KakaoLocalClient {

    private static final int KAKAO_MAX_PAGE_SIZE = 15;

    private final RestClient restClient;
    private final KakaoLocalProperties properties;

    public KakaoLocalClient(
            @Qualifier("kakaoLocalRestClient") RestClient restClient,
            KakaoLocalProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Optional<KakaoGeocodedAddress> geocode(String address) {
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        JsonNode documents = getDocuments("/v2/local/search/address.json", builder -> builder
                .queryParam("query", address)
                .queryParam("size", 1)
                .build());
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        JsonNode document = documents.get(0);
        BigDecimal longitude = decimal(document, "x");
        BigDecimal latitude = decimal(document, "y");
        if (latitude == null || longitude == null) {
            throw invalidResponse();
        }
        return Optional.of(new KakaoGeocodedAddress(
                latitude,
                longitude,
                nestedText(document, "address", "address_name"),
                nestedText(document, "road_address", "address_name")
        ));
    }

    public Optional<KakaoReverseAddress> reverse(BigDecimal latitude, BigDecimal longitude) {
        JsonNode documents = getDocuments("/v2/local/geo/coord2address.json", builder -> builder
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("input_coord", "WGS84")
                .build());
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        JsonNode document = documents.get(0);
        JsonNode address = document.path("address");
        return Optional.of(new KakaoReverseAddress(
                text(address, "address_name"),
                nestedText(document, "road_address", "address_name"),
                text(address, "region_1depth_name"),
                text(address, "region_2depth_name"),
                text(address, "region_3depth_name")
        ));
    }

    public List<KakaoParkingPlace> findParking(
            BigDecimal latitude,
            BigDecimal longitude,
            int radiusMeters,
            int limit
    ) {
        int size = Math.min(Math.max(limit, 1), KAKAO_MAX_PAGE_SIZE);
        JsonNode documents = getDocuments("/v2/local/search/category.json", builder -> builder
                .queryParam("category_group_code", "PK6")
                .queryParam("x", longitude.toPlainString())
                .queryParam("y", latitude.toPlainString())
                .queryParam("radius", radiusMeters)
                .queryParam("sort", "distance")
                .queryParam("size", size)
                .build());

        List<KakaoParkingPlace> result = new ArrayList<>();
        for (JsonNode document : documents) {
            BigDecimal parkingLongitude = decimal(document, "x");
            BigDecimal parkingLatitude = decimal(document, "y");
            String id = text(document, "id");
            String name = text(document, "place_name");
            if (parkingLatitude == null || parkingLongitude == null
                    || !StringUtils.hasText(id) || !StringUtils.hasText(name)) {
                continue;
            }
            result.add(new KakaoParkingPlace(
                    id,
                    name,
                    firstText(document, "road_address_name", "address_name"),
                    parkingLatitude,
                    parkingLongitude,
                    integer(document, "distance")
            ));
        }
        return List.copyOf(result);
    }

    private JsonNode getDocuments(String path, Function<UriBuilder, java.net.URI> uriFunction) {
        if (!properties.hasRestApiKey()) {
            throw new KakaoLocalException("카카오 Local REST API 키가 설정되지 않았습니다.", false);
        }
        try {
            JsonNode root = restClient.get()
                    .uri(builder -> uriFunction.apply(builder.path(path)))
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !root.path("documents").isArray()) {
                throw invalidResponse();
            }
            return root.path("documents");
        } catch (KakaoLocalException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            boolean retryable = exception.getStatusCode().value() == 429
                    || exception.getStatusCode().is5xxServerError();
            throw new KakaoLocalException("카카오 Local API가 요청을 처리하지 못했습니다.", retryable);
        } catch (RestClientException exception) {
            throw new KakaoLocalException("카카오 Local API 호출에 실패했습니다.", true);
        }
    }

    private static KakaoLocalException invalidResponse() {
        return new KakaoLocalException("카카오 Local API 응답 형식이 올바르지 않습니다.", true);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(7, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String nestedText(JsonNode node, String object, String field) {
        return text(node.path(object), field);
    }

    private static String firstText(JsonNode node, String first, String second) {
        String value = text(node, first);
        return StringUtils.hasText(value) ? value : text(node, second);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : null;
    }
}
