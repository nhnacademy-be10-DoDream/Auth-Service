package shop.dodream.authservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.TokenResponse;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.dto.payco.*;
import shop.dodream.authservice.jwt.JwtProperties;
import shop.dodream.authservice.jwt.JwtTokenProvider;
import shop.dodream.authservice.repository.RefreshTokenRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaycoOAuthService {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserFeignClient userFeignClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final PaycoProperties paycoProperties;

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder
                .fromHttpUrl(paycoProperties.getAuthorizationUri())
                .queryParam("response_type","code")
                .queryParam("client_id",paycoProperties.getClientId())
                .queryParam("redirect_uri",paycoProperties.getRedirectUri())
                .queryParam("state", UUID.randomUUID().toString())
                .queryParam("serviceProviderCode","FRIENDS")
                .queryParam("userLocale","ko_KR")
                .build().toUriString();
    }

    public TokenResponse loginWithPayco(String code, String state) {
        String accessToken = requestAccessToken(code,state);
        PaycoUserInfo info = requestUserInfo(accessToken);
        UserResponse user = findOrCreateUser(info);
        String accessJwt = jwtTokenProvider.createAccessToken(user.getUserId(),user.getRole());
        String refreshJwt = jwtTokenProvider.createRefreshToken(user.getUserId());
        refreshTokenRepository.save(user.getUserId(), refreshJwt);
        return new TokenResponse(accessJwt,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),refreshJwt);
    }

    private String requestAccessToken(String code, String state){
        String url = paycoProperties.getTokenUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", paycoProperties.getClientId());
        params.add("client_secret", paycoProperties.getClientSecret());
        params.add("code", code);
        params.add("state", state);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<PaycoTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, PaycoTokenResponse.class);

        return response.getBody().getAccessToken();
    }

    private PaycoUserInfo requestUserInfo(String accessToken) {
        String url = paycoProperties.getUserInfoUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("client_id", paycoProperties.getClientId());
        headers.add("access_token", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PaycoUserResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, PaycoUserResponse.class);

        return response.getBody().getData().getMember();
    }

    private UserResponse findOrCreateUser(PaycoUserInfo info) {
        String userId = "payco_" + info.getEmail();

        try {
            return userFeignClient.findByUserId(userId);
        } catch (FeignException.NotFound e) {
            String password = passwordEncoder.encode(UUID.randomUUID().toString());
            Date birthDate = null;

            if (info.getBirthday() != null && info.getBirthday().length() == 4) {
                try {
                    birthDate = new SimpleDateFormat("yyyy-MM-dd").parse("2000-" + info.getBirthday().substring(0, 2) + "-" + info.getBirthday().substring(2));
                } catch (ParseException ex) {
                    birthDate = null;
                }
            }

            PaycoUserRequest request = new PaycoUserRequest(
                    userId,
                    password,
                    info.getEmail(),
                    info.getName(),
                    info.getMobile(),
                    birthDate
            );

            return userFeignClient.createPaycoUser(request);
        }
    }
}
