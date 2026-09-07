package com.danyeogam.backend.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoLocalClientTest {

    private MockRestServiceServer server;
    private KakaoLocalClient client;
    private KakaoLocalProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KakaoLocalProperties();
        properties.setBaseUrl("https://kakao-local.test");
        properties.setRestApiKey("test-rest-key");
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoLocalClient(builder.build(), properties);
    }

    @Test
    void geocodesAddressWithServerSideRestKey() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/search/address.json")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-key"))
                .andExpect(queryParam(
                        "query",
                        "%EC%84%9C%EC%9A%B8%ED%8A%B9%EB%B3%84%EC%8B%9C%20%EC%A2%85%EB%A1%9C%EA%B5%AC%20%EC%82%AC%EC%A7%81%EB%A1%9C%20161"
                ))
                .andExpect(queryParam("size", "1"))
                .andRespond(withSuccess("""
                        {"documents":[{
                          "x":"126.9769930","y":"37.5788222",
                          "address":{"address_name":"서울 종로구 세종로 1"},
                          "road_address":{"address_name":"서울 종로구 사직로 161"}
                        }]}
                        """, MediaType.APPLICATION_JSON));

        KakaoGeocodedAddress result = client.geocode("서울특별시 종로구 사직로 161").orElseThrow();

        assertThat(result.latitude()).isEqualByComparingTo("37.5788222");
        assertThat(result.longitude()).isEqualByComparingTo("126.9769930");
        assertThat(result.roadAddressName()).isEqualTo("서울 종로구 사직로 161");
        server.verify();
    }

    @Test
    void reverseGeocodesAndReadsNearbyParking() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/geo/coord2address.json")))
                .andExpect(queryParam("x", "127.1480"))
                .andExpect(queryParam("y", "35.8242"))
                .andExpect(queryParam("input_coord", "WGS84"))
                .andRespond(withSuccess("""
                        {"documents":[{
                          "address":{"address_name":"전북 전주시 완산구 풍남동3가",
                            "region_1depth_name":"전북특별자치도","region_2depth_name":"전주시 완산구",
                            "region_3depth_name":"풍남동3가"},
                          "road_address":{"address_name":"전북 전주시 완산구 태조로 1"}
                        }]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/search/category.json")))
                .andExpect(queryParam("category_group_code", "PK6"))
                .andExpect(queryParam("radius", "3000"))
                .andExpect(queryParam("sort", "distance"))
                .andExpect(queryParam("size", "3"))
                .andRespond(withSuccess("""
                        {"documents":[{
                          "id":"123456","place_name":"한옥마을 주차장",
                          "address_name":"전북 전주시 완산구 풍남동","road_address_name":"",
                          "x":"127.151","y":"35.816","distance":"260"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        KakaoReverseAddress reverse = client.reverse(
                new BigDecimal("35.8242"), new BigDecimal("127.1480")
        ).orElseThrow();
        KakaoParkingPlace parking = client.findParking(
                new BigDecimal("35.8242"), new BigDecimal("127.1480"), 3000, 3
        ).get(0);

        assertThat(reverse.depth1()).isEqualTo("전북특별자치도");
        assertThat(reverse.roadAddressName()).isEqualTo("전북 전주시 완산구 태조로 1");
        assertThat(parking.id()).isEqualTo("123456");
        assertThat(parking.address()).isEqualTo("전북 전주시 완산구 풍남동");
        assertThat(parking.distanceMeters()).isEqualTo(260);
        server.verify();
    }

    @Test
    void missingKeyFailsBeforeAnyHttpRequest() {
        properties.setRestApiKey("");

        assertThatThrownBy(() -> client.reverse(BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(KakaoLocalException.class)
                .hasMessageContaining("키가 설정되지 않았습니다");
        server.verify();
    }
}
