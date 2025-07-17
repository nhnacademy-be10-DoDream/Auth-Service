package shop.dodream.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenInfo {
    private String token;
    private String userAgent;
    private String ip;
}
