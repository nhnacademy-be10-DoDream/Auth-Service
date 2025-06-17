package shop.dodream.authservice.dto.payco;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaycoTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("access_token_secret")
    private String accessTokenSecret;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("state")
    private String state;
}
