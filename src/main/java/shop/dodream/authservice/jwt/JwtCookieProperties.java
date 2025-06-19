package shop.dodream.authservice.jwt;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "jwt.cookie")
@Component
public class JwtCookieProperties {
    private boolean secure;
    private String sameSite;
}
