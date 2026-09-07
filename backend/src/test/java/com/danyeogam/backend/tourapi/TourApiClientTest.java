package com.danyeogam.backend.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TourApiClientTest {

    private MockRestServiceServer server;
    private TourApiClient client;

    @BeforeEach
    void setUp() {
        TourApiProperties properties = new TourApiProperties();
        properties.setBaseUrl("https://tour-api.test");
        properties.setServiceKey("test-service-key");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TourApiClient(builder.build(), properties);
    }

    @Test
    void readsAreaBasedTouristSummariesAndRepresentativeImages() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/areaBasedList2")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("serviceKey", "test-service-key"))
                .andExpect(queryParam("MobileOS", "ETC"))
                .andExpect(queryParam("MobileApp", "%EB%8B%A4%EB%85%80%EA%B0%90"))
                .andExpect(queryParam("_type", "json"))
                .andExpect(queryParam("pageNo", "1"))
                .andExpect(queryParam("numOfRows", "10"))
                .andExpect(queryParam("areaCode", "1"))
                .andExpect(queryParam("contentTypeId", "12"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": [{
                                "contentid": "126508",
                                "contenttypeid": "12",
                                "title": "경복궁",
                                "addr1": "서울특별시 종로구 사직로 161",
                                "addr2": "",
                                "areacode": "1",
                                "sigungucode": "23",
                                "mapx": "126.9769930325",
                                "mapy": "37.5788222356",
                                "firstimage": "https://image.test/original.jpg",
                                "firstimage2": "https://image.test/thumbnail.jpg",
                                "modifiedtime": "20260101000000"
                              }]},
                              "numOfRows": 10,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TouristSummary> page = client.getAreaBasedList(1, 10, "1", "12");

        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(spot -> {
            assertThat(spot.contentId()).isEqualTo("126508");
            assertThat(spot.title()).isEqualTo("경복궁");
            assertThat(spot.firstImageUrl()).isEqualTo("https://image.test/original.jpg");
            assertThat(spot.firstThumbnailUrl()).isEqualTo("https://image.test/thumbnail.jpg");
        });
        server.verify();
    }

    @Test
    void prefersLegalDongCodesAndConvertsLegacyProvinceWhenNecessary() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/areaBasedList2")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": [
                                {
                                  "contentid": "modern",
                                  "title": "현행 코드 관광지",
                                  "areacode": "1",
                                  "sigungucode": "23",
                                  "lDongRegnCd": "11",
                                  "lDongSignguCd": "110"
                                },
                                {
                                  "contentid": "legacy",
                                  "title": "구 코드 관광지",
                                  "areacode": "31",
                                  "sigungucode": "1"
                                },
                                {
                                  "contentid": "sejong",
                                  "title": "세종 관광지",
                                  "lDongRegnCd": "36110",
                                  "lDongSignguCd": "36110"
                                }
                              ]},
                              "numOfRows": 10,
                              "pageNo": 1,
                              "totalCount": 3
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TouristSummary> page = client.getAreaBasedList(1, 10, null);

        assertThat(page.items().get(0).areaCode()).isEqualTo("11");
        assertThat(page.items().get(0).districtCode()).isEqualTo("110");
        assertThat(page.items().get(1).areaCode()).isEqualTo("41");
        assertThat(page.items().get(1).districtCode()).isNull();
        assertThat(page.items().get(2).areaCode()).isEqualTo("36");
        assertThat(page.items().get(2).districtCode()).isNull();
        server.verify();
    }

    @Test
    void readsDetailImagesAndCopyrightType() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/detailImage2")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("contentId", "126508"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": {
                                "contentid": "126508",
                                "imgname": "경복궁 전경",
                                "originimgurl": "https://image.test/palace.jpg",
                                "smallimageurl": "https://image.test/palace-small.jpg",
                                "serialnum": "12345",
                                "cpyrhtDivCd": "Type1"
                              }},
                              "numOfRows": 20,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TouristImage> page = client.getImages("126508", 1, 20);

        assertThat(page.items()).singleElement().satisfies(image -> {
            assertThat(image.originalUrl()).isEqualTo("https://image.test/palace.jpg");
            assertThat(image.thumbnailUrl()).isEqualTo("https://image.test/palace-small.jpg");
            assertThat(image.copyrightType()).isEqualTo("Type1");
        });
        server.verify();
    }

    @Test
    void readsAreaCodesAndDistrictCodes() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/areaCode2")))
                .andExpect(queryParam("areaCode", "1"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": [{"code": "23", "name": "종로구"}]},
                              "numOfRows": 100,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TourRegionCode> page = client.getAreaCodes("1", 1, 100);

        assertThat(page.items()).containsExactly(new TourRegionCode("23", "종로구"));
        server.verify();
    }

    @Test
    void readsLegalDongProvinceAndDistrictCodes() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/ldongCode2")))
                .andExpect(queryParam("lDongRegnCd", "11"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": [{"code": "110", "name": "종로구"}]},
                              "numOfRows": 100,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TourRegionCode> page = client.getLegalDongCodes("11", 1, 100);

        assertThat(page.items()).containsExactly(new TourRegionCode("110", "종로구"));
        server.verify();
    }

    @Test
    void normalizesSejongLegalDongProvinceCode() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/ldongCode2")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": [{"code": "36110", "name": "세종특별자치시"}]},
                              "numOfRows": 100,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TourRegionCode> page = client.getLegalDongCodes(null, 1, 100);

        assertThat(page.items()).containsExactly(new TourRegionCode("36", "세종특별자치시"));
        server.verify();
    }

    @Test
    void readsCommonDetailWithOverview() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/detailCommon2")))
                .andExpect(queryParam("contentId", "126508"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {
                              "items": {"item": {
                                "contentid": "126508",
                                "title": "경복궁",
                                "addr1": "서울특별시 종로구 사직로 161",
                                "mapx": "126.9769",
                                "mapy": "37.5788",
                                "overview": "조선 왕조의 법궁",
                                "homepage": "https://royal.khs.go.kr/",
                                "tel": "02-0000-0000",
                                "firstimage": "https://image.test/original.jpg",
                                "firstimage2": "https://image.test/thumbnail.jpg",
                                "modifiedtime": "20260101000000",
                                "cpyrhtDivCd": "Type1"
                              }},
                              "numOfRows": 10,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TouristDetail detail = client.getCommonDetail("126508").orElseThrow();

        assertThat(detail.overview()).isEqualTo("조선 왕조의 법궁");
        assertThat(detail.homepage()).isEqualTo("https://royal.khs.go.kr/");
        server.verify();
    }

    @Test
    void returnsAnEmptyListWhenTourApiHasNoImages() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/detailImage2")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "0000", "resultMsg": "OK"},
                            "body": {"items": "", "numOfRows": 20, "pageNo": 1, "totalCount": 0}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourApiPage<TouristImage> page = client.getImages("no-image", 1, 20);

        assertThat(page.items()).isEmpty();
        server.verify();
    }

    @Test
    void convertsTourApiFailureHeaderToAnExceptionWithoutExposingTheKey() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/areaBasedList2")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "22", "resultMsg": "LIMIT EXCEEDED"},
                            "body": {}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getAreaBasedList(1, 10, null))
                .isInstanceOf(TourApiException.class)
                .hasMessage("TourAPI 오류: LIMIT EXCEEDED")
                .hasMessageNotContaining("test-service-key");
        server.verify();
    }

    @Test
    void convertsTopLevelParameterErrorToAnException() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/detailImage2")))
                .andRespond(withSuccess("""
                        {
                          "responseTime": "2026-09-02T20:17:05.374",
                          "resultCode": "10",
                          "resultMsg": "INVALID_REQUEST_PARAMETER_ERROR"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getImages("126508", 1, 20))
                .isInstanceOf(TourApiException.class)
                .hasMessage("TourAPI 오류: INVALID_REQUEST_PARAMETER_ERROR");
        server.verify();
    }

    @Test
    void httpFailureDoesNotRetainARequestUrlOrServiceKeyAsItsCause() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/areaBasedList2")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getAreaBasedList(1, 10, null))
                .isInstanceOf(TourApiException.class)
                .hasMessage("TourAPI 호출에 실패했습니다.")
                .hasMessageNotContaining("test-service-key")
                .hasNoCause();
        server.verify();
    }

    @Test
    void rejectsMissingServiceKeyBeforeMakingARequest() {
        TourApiProperties properties = new TourApiProperties();
        properties.setBaseUrl("https://tour-api.test");
        TourApiClient clientWithoutKey = new TourApiClient(
                RestClient.builder().baseUrl(properties.getBaseUrl()).build(),
                properties
        );

        assertThatThrownBy(() -> clientWithoutKey.getAreaBasedList(1, 10, null))
                .isInstanceOf(TourApiException.class)
                .hasMessageContaining("서비스 키가 설정되지 않았습니다");
    }
}
