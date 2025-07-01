package shop.dodream.authservice.dto.payco;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payco")
public class PaycoProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
}
