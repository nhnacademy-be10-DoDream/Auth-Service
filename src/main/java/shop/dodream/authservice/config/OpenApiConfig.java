package shop.dodream.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("인증 서비스 API")
                        .version("v1.0")
                        .description("회원 인증, JWT 발급을 제공하는 API 문서입니다.")
                )
                .tags(List.of(
                        new Tag().name("Auth").description("로그인 및 인증 API"),
                        new Tag().name("Auth/Dormant").description("휴면 계정 인증 API")
                ));
    }
}
