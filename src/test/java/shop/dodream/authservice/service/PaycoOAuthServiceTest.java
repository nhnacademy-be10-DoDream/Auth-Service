package shop.dodream.authservice.service;


import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.*;
import shop.dodream.authservice.dto.payco.*;
import shop.dodream.authservice.exception.AccountException;
import shop.dodream.authservice.jwt.JwtTokenProvider;
import shop.dodream.authservice.jwt.properties.JwtProperties;
import shop.dodream.authservice.repository.TokenRepository;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaycoOAuthServiceTest {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PaycoProperties paycoProperties;

    @InjectMocks
    private PaycoOAuthService paycoService;

    @Mock
    HttpServletRequest httpRequest;

    @BeforeEach
    public void setUp() {
        given(jwtProperties.getAccessTokenExpiration()).willReturn(3_600_000L);
        given(paycoProperties.getAuthorizationUri()).willReturn("https://payco/auth");
        given(paycoProperties.getClientId()).willReturn("CLIENT_ID");
        given(paycoProperties.getRedirectUri()).willReturn("https://app/callback");
        given(paycoProperties.getTokenUri()).willReturn("https://payco/token");
        given(paycoProperties.getUserInfoUri()).willReturn("https://payco/userinfo");
        given(paycoProperties.getClientSecret()).willReturn("SECRET");
    }
    @Test
    void buildAuthorizationUrl_includesAllParameters() {
        String url = paycoService.buildAuthorizationUrl();
        var params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();

        assertThat(params.get("response_type")).containsExactly("code");
        assertThat(params.get("client_id")).containsExactly("CLIENT_ID");
        assertThat(params.get("redirect_uri")).containsExactly("https://app/callback");
        assertThat(params).containsKey("state");
        assertThat(params.get("serviceProviderCode")).containsExactly("FRIENDS");
        assertThat(params.get("userLocale")).containsExactly("ko_KR");
    }

    @Test
    void loginWithPayco_existingUser_succeeds() {
        PaycoTokenResponse tokenResp = new PaycoTokenResponse();
        tokenResp.setAccessToken("PAYCO_AT");
        given(restTemplate.exchange(
                eq("https://payco/token"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(PaycoTokenResponse.class))
        ).willReturn(ResponseEntity.ok(tokenResp));

        PaycoUserInfo info = new PaycoUserInfo();
        info.setEmail("u@e.com");
        info.setName("Name");
        info.setMobile("01012345678");
        info.setBirthday("0101");
        PaycoUserResponse upr = new PaycoUserResponse();
        PaycoUserData data = new PaycoUserData();
        data.setMember(info);
        upr.setData(data);
        given(restTemplate.exchange(
                eq("https://payco/userinfo"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(PaycoUserResponse.class))
        ).willReturn(ResponseEntity.ok(upr));

        String paycoId = "payco_u@e.com";
        UserResponse existing = new UserResponse(paycoId, "pwd", Role.USER, Status.ACTIVE);
        given(userFeignClient.findByUserId(paycoId)).willReturn(existing);

        given(jwtTokenProvider.createAccessToken(anyString())).willReturn("AT");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("RT");
        given(httpRequest.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA");
        given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
        given(httpRequest.getRemoteAddr()).willReturn("IP");

        TokenResponse tr = paycoService.loginWithPayco("code", "state", httpRequest);

        assertThat(tr.getAccessToken()).isEqualTo("AT");
        assertThat(tr.getRefreshToken()).isEqualTo("RT");
        then(tokenRepository).should()
                .save(anyString(), any(SessionUser.class), eq("RT"), eq("UA"), eq("IP"));
        then(userFeignClient).should().updateLastLogin(paycoId);
    }

    @Test
    void loginWithPayco_newUser_succeeds() {
        PaycoTokenResponse tokenResp = new PaycoTokenResponse();
        tokenResp.setAccessToken("TOKEN");
        given(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(PaycoTokenResponse.class))
        ).willReturn(ResponseEntity.ok(tokenResp));
        PaycoUserInfo info = new PaycoUserInfo();
        info.setEmail("new@e.com"); info.setBirthday("0202");
        PaycoUserResponse upr = new PaycoUserResponse();
        PaycoUserData data = new PaycoUserData();
        data.setMember(info);
        upr.setData(data);
        given(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(), eq(PaycoUserResponse.class))
        ).willReturn(ResponseEntity.ok(upr));

        Request req = Request.create(Request.HttpMethod.GET, "", Collections.emptyMap(), null, StandardCharsets.UTF_8);
        Response resp = Response.builder().status(404).request(req).build();
        given(userFeignClient.findByUserId("payco_new@e.com"))
                .willThrow(FeignException.errorStatus("userClient", resp));

        UserResponse created = new UserResponse("payco_new@e.com", "pwd", Role.USER, Status.ACTIVE);
        given(userFeignClient.createPaycoUser(any(PaycoUserRequest.class))).willReturn(created);

        given(passwordEncoder.encode(anyString())).willReturn("ENC");
        given(jwtTokenProvider.createAccessToken(anyString())).willReturn("AT2");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("RT2");
        given(httpRequest.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA2");
        given(httpRequest.getHeader("X-Forwarded-For")).willReturn("1.2.3.4, 5.6.7.8");

        TokenResponse tr = paycoService.loginWithPayco("c2", "s2", httpRequest);

        then(userFeignClient).should().createPaycoUser(argThat(reqDto ->
                reqDto.getUserId().equals("payco_new@e.com") &&
                        reqDto.getEmail().equals("new@e.com")
        ));
        assertThat(tr.getAccessToken()).isEqualTo("AT2");
        then(tokenRepository).should()
                .save(anyString(), any(SessionUser.class), eq("RT2"), eq("UA2"), eq("1.2.3.4"));
    }

    @Test
    void loginWithPayco_dormant_throwsAccountException() {
        given(restTemplate.exchange(anyString(), any(), any(), eq(PaycoTokenResponse.class)))
                .willReturn(ResponseEntity.ok(new PaycoTokenResponse()));
        PaycoUserResponse upr = new PaycoUserResponse();
        PaycoUserData data = new PaycoUserData();
        data.setMember(new PaycoUserInfo());
        upr.setData(data);
        given(restTemplate.exchange(anyString(), any(), any(), eq(PaycoUserResponse.class)))
                .willReturn(ResponseEntity.ok(upr));

        given(userFeignClient.findByUserId(anyString()))
                .willReturn(new UserResponse("d@e", "p", Role.USER, Status.DORMANT));

        assertThatThrownBy(() ->
                paycoService.loginWithPayco("code", "state", httpRequest))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("휴면 계정입니다");
    }

    @Test
    void loginWithPayco_withdrawn_throwsAccountException() {
        given(restTemplate.exchange(anyString(), any(), any(), eq(PaycoTokenResponse.class)))
                .willReturn(ResponseEntity.ok(new PaycoTokenResponse()));
        PaycoUserResponse upr = new PaycoUserResponse();
        PaycoUserData data = new PaycoUserData();
        data.setMember(new PaycoUserInfo());
        upr.setData(data);
        given(restTemplate.exchange(anyString(), any(), any(), eq(PaycoUserResponse.class)))
                .willReturn(ResponseEntity.ok(upr));

        given(userFeignClient.findByUserId(anyString()))
                .willReturn(new UserResponse("w@e", "p", Role.USER, Status.WITHDRAWN));

        assertThatThrownBy(() ->
                paycoService.loginWithPayco("code", "state", httpRequest))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("탈퇴된 계정입니다");
    }

}
