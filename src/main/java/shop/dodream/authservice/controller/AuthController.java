package shop.dodream.authservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.*;
import shop.dodream.authservice.exception.AccountException;
import shop.dodream.authservice.jwt.JwtCookieProperties;
import shop.dodream.authservice.jwt.JwtTokenProvider;
import shop.dodream.authservice.repository.RefreshTokenRepository;
import shop.dodream.authservice.service.PaycoOAuthService;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserFeignClient userFeignClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PaycoOAuthService paycoOAuthService;
    private final JwtCookieProperties jwtCookieProperties;


    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Validated @RequestBody LoginRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
        );

        UserResponse user = userFeignClient.findByUserId(request.getUserId());

        if(user.getStatus() == Status.DORMANT){
            throw new AccountException("휴면 계정입니다. 인증 후 로그인 해주세요.",Status.DORMANT, user.getUserId());
        }else if(user.getStatus() == Status.WITHDRAWN){
            throw new AccountException("탈퇴된 계정입니다. 다른 아이디로 로그인 해주세요.",Status.WITHDRAWN, user.getUserId());
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        refreshTokenRepository.save(user.getUserId(), refreshToken);
        addTokenCookie(response,accessToken);
        addRefreshTokenCookie(response,refreshToken);
        userFeignClient.updateLastLogin(user.getUserId());

        return ResponseEntity.ok().build();
    }

    // access토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken,HttpServletResponse response) {
        if(!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        if(!refreshTokenRepository.isValid(userId, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Role role = userFeignClient.findByUserId(userId).getRole();
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);

        addTokenCookie(response,newAccessToken);

        return ResponseEntity.ok().build();
    }

    // Payco 로그인 URL 리턴
    @GetMapping("/payco/authorize")
    public void getAuthorizationUrl(HttpServletResponse response) throws IOException {
        String url = paycoOAuthService.buildAuthorizationUrl();
        response.sendRedirect(url);
    }

    // Payco Redirect 이후 code + state 전달 → JWT 발급
    @PostMapping("/payco/callback")
    public ResponseEntity<Void> handlePaycoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = paycoOAuthService.loginWithPayco(code, state);
        addTokenCookie(response,tokenResponse.getAccessToken());
        addRefreshTokenCookie(response,tokenResponse.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                       HttpServletResponse response) {
        if (refreshToken != null) {
            refreshTokenRepository.delete(refreshToken);
        }
        ResponseCookie accessTokenClear = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenClear.toString());

        ResponseCookie refreshTokenClear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenClear.toString());

        return ResponseEntity.noContent().build();
    }

}
