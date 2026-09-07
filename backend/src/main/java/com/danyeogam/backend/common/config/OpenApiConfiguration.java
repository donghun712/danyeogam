package com.danyeogam.backend.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "다녀감 Backend API",
                version = "v1",
                description = "전국 관광지 탐색, 주변 주차장, 익명 세션, GPS 스탬프와 지역 도감 API",
                contact = @Contact(name = "다녀감 백엔드 팀"),
                license = @License(name = "Private competition project")
        )
)
@SecurityScheme(
        name = "anonymousSession",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "dg_session",
        description = "POST /api/v1/sessions/anonymous 호출로 발급되는 HttpOnly 익명 세션 쿠키"
)
public class OpenApiConfiguration {
}
