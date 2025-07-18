package shop.dodream.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.*;
import shop.dodream.authservice.exception.AccountException;
import shop.dodream.authservice.exception.AuthException;
import shop.dodream.authservice.jwt.JwtTokenProvider;
import shop.dodream.authservice.jwt.properties.JwtProperties;
import shop.dodream.authservice.repository.TokenRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
public class AuthServiceTest {

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenRepository tokenRepository;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private UserFeignClient userFeignClient;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600_000L);
    }

    @Test
    void login_success(){
        LoginRequest loginRequest = new LoginRequest("user1","password");
        UserResponse user = new UserResponse("user1", new BCryptPasswordEncoder().encode("password"), Role.USER, Status.ACTIVE);
        HttpServletRequest req = mock(HttpServletRequest.class);

        given(userFeignClient.findByUserId("user1")).willReturn(user);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("AT");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("RT");
        given(req.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA");
        given(req.getRemoteAddr()).willReturn("IP");

        TokenResponse res = authService.login(loginRequest, req);

        assertThat(res.getAccessToken()).isEqualTo("AT");
        assertThat(res.getRefreshToken()).isEqualTo("RT");
        then(tokenRepository).should().save(any(),any(),eq("RT"),eq("UA"),eq("IP"));
    }

    @Test
    void login_forwardedFor_first() {
        LoginRequest req = new LoginRequest("user1", "pw");
        UserResponse user = new UserResponse("user1", "pw", Role.USER, Status.ACTIVE);
        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        given(authenticationManager.authenticate(any()))
                .willReturn(new UsernamePasswordAuthenticationToken("user1","pw"));
        given(userFeignClient.findByUserId("user1")).willReturn(user);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("AT");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("RT");
        given(httpReq.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA");
        given(httpReq.getHeader("X-Forwarded-For")).willReturn("1.2.3.4, 5.6.7.8");

        authService.login(req, httpReq);

        then(tokenRepository).should().save(
                anyString(), any(SessionUser.class), anyString(), eq("UA"), eq("1.2.3.4")
        );
    }

    @Test
    void login_dormant_throws() {
        LoginRequest req = new LoginRequest("user1", "pw");
        UserResponse user = new UserResponse("user1", "encoded", Role.USER, Status.DORMANT);
        given(userFeignClient.findByUserId("user1")).willReturn(user);

        assertThatThrownBy(() -> authService.login(req, mock(HttpServletRequest.class)))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("휴면 계정입니다");
    }
    @Test
    void login_withdrawn_throws() {
        LoginRequest req = new LoginRequest("user1", "pw");
        UserResponse user = new UserResponse("user1", "encoded", Role.USER, Status.WITHDRAWN);
        given(userFeignClient.findByUserId("user1")).willReturn(user);

        assertThatThrownBy(() -> authService.login(req, mock(HttpServletRequest.class)))
                .isInstanceOf(AccountException.class)
                .hasMessageContaining("탈퇴된 계정입니다");
    }

    @Test
    void refresh_success() {
        String RT = "refresh";
        String uuid = "uuid";
        SessionUser su = new SessionUser("user1", Role.USER);
        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        given(jwtTokenProvider.validateToken(RT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(RT)).willReturn(uuid);
        given(tokenRepository.findByUuid(uuid)).willReturn(su);
        given(httpReq.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA");
        given(httpReq.getRemoteAddr()).willReturn("IP");
        given(tokenRepository.isValid(uuid, su, RT, "UA", "IP")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(uuid)).willReturn("newAT");

        TokenResponse res = authService.refresh(RT, httpReq);

        assertThat(res.getAccessToken()).isEqualTo("newAT");
    }
    @Test
    void refresh_noToken_throws() {
        assertThatThrownBy(() -> authService.refresh(null, mock(HttpServletRequest.class)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("리프레시 토큰이 없습니다");
    }

    @Test
    void refresh_invalidToken_throws() {
        given(jwtTokenProvider.validateToken("bad")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad", mock(HttpServletRequest.class)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("유효하지 않습니다");
    }

    @Test
    void refresh_sessionExpired_throws() {
        String RT = "refresh";
        String uuid = "uuid";
        given(jwtTokenProvider.validateToken(RT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(RT)).willReturn(uuid);
        given(tokenRepository.findByUuid(uuid)).willReturn(null);

        assertThatThrownBy(() -> authService.refresh(RT, mock(HttpServletRequest.class)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("세션이 만료되었습니다");
    }

    @Test
    void refresh_notValidSession_throws() {
        String RT = "refresh";
        String uuid = "uuid";
        SessionUser su = new SessionUser("user1", Role.USER);
        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        given(jwtTokenProvider.validateToken(RT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(RT)).willReturn(uuid);
        given(tokenRepository.findByUuid(uuid)).willReturn(su);
        given(httpReq.getHeader(HttpHeaders.USER_AGENT)).willReturn("UA");
        given(httpReq.getRemoteAddr()).willReturn("IP");
        given(tokenRepository.isValid(uuid, su, RT, "UA", "IP")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh(RT, httpReq))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("유효하지 않습니다");
    }


    @Test
    void logout_validToken_deletesSessionAndClearsCookies() {
        String RT = "refresh";
        HttpServletResponse httpRes = mock(HttpServletResponse.class);
        given(jwtTokenProvider.validateToken(RT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(RT)).willReturn("uuid");

        authService.logout(RT, httpRes);

        then(tokenRepository).should().delete("uuid");
        then(httpRes).should(atLeastOnce())
                .addHeader(eq(HttpHeaders.SET_COOKIE), contains("Max-Age=0"));
    }

    @Test
    void logout_noToken_justClearsCookies() {
        HttpServletResponse httpRes = mock(HttpServletResponse.class);

        authService.logout(null, httpRes);

        then(tokenRepository).should(never()).delete(any());
        then(httpRes).should(atLeastOnce())
                .addHeader(eq(HttpHeaders.SET_COOKIE), contains("Max-Age=0"));
    }
    @Test
    void getSessionUser_success() {
        String AT = "access";
        String uuid = "uuid";
        SessionUser su = new SessionUser("user1", Role.USER);
        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        given(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer " + AT);
        given(jwtTokenProvider.validateToken(AT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(AT)).willReturn(uuid);
        given(tokenRepository.findByUuid(uuid)).willReturn(su);

        SessionUser result = authService.getSessionUser(httpReq);

        assertThat(result).isEqualTo(su);
    }

    @Test
    void getSessionUser_noToken_throws() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        given(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(null);

        assertThatThrownBy(() -> authService.getSessionUser(httpReq))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("유효한 토큰이 아닙니다");
    }

    @Test
    void getSessionUser_invalidToken_throws() {
        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        given(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer bad");
        given(jwtTokenProvider.validateToken("bad")).willReturn(false);

        assertThatThrownBy(() -> authService.getSessionUser(httpReq))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("유효한 토큰이 아닙니다");
    }

    @Test
    void getSessionUser_sessionExpired_throws() {
        String AT = "access";
        HttpServletRequest httpReq = mock(HttpServletRequest.class);

        given(httpReq.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer " + AT);
        given(jwtTokenProvider.validateToken(AT)).willReturn(true);
        given(jwtTokenProvider.getUuidFromToken(AT)).willReturn("uuid");
        given(tokenRepository.findByUuid("uuid")).willReturn(null);

        assertThatThrownBy(() -> authService.getSessionUser(httpReq))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("세션이 만료되었습니다");
    }

}
