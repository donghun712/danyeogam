package com.danyeogam.backend.common;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.danyeogam.backend.common.api.ApiResponse;
import com.danyeogam.backend.common.api.RequestIdFilter;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.common.error.GlobalExceptionHandler;
import com.danyeogam.backend.common.error.RateLimitException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class CommonApiWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void successResponseContainsRequestIdAndGeneratedAt() throws Exception {
        mockMvc.perform(get("/test/success").header(RequestIdFilter.HEADER_NAME, "front-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "front-request-1"))
                .andExpect(jsonPath("$.data.value").value("ok"))
                .andExpect(jsonPath("$.meta.requestId").value("front-request-1"))
                .andExpect(jsonPath("$.meta.generatedAt").exists());
    }

    @Test
    void unsafeRequestIdIsReplaced() throws Exception {
        mockMvc.perform(get("/test/success").header(RequestIdFilter.HEADER_NAME, "unsafe request id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestIdFilter.HEADER_NAME,
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                ))
                .andExpect(jsonPath("$.meta.requestId", matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                )));
    }

    @Test
    void validationFailureUsesStableErrorCodeAndFieldList() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void malformedJsonDoesNotExposeParserDetails() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    void unsupportedContentTypeUsesStable415ErrorResponse() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void businessExceptionUsesItsHttpStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.retryable").value(false));
    }

    @Test
    void rateLimitResponseIncludesRetryAfterHeader() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "37"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.error.retryable").value(true));
    }

    @RestController
    public static class TestController {

        @GetMapping("/test/success")
        ApiResponse<Map<String, String>> success() {
            return ApiResponse.success(Map.of("value", "ok"));
        }

        @PostMapping("/test/validation")
        ApiResponse<ValidationRequest> validation(@Valid @RequestBody ValidationRequest request) {
            return ApiResponse.success(request);
        }

        @GetMapping("/test/not-found")
        ApiResponse<Void> notFound() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @GetMapping("/test/rate-limit")
        ApiResponse<Void> rateLimit() {
            throw new RateLimitException(37);
        }
    }

    record ValidationRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }
}
