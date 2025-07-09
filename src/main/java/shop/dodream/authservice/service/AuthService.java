package shop.dodream.authservice.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.*;
import shop.dodream.authservice.exception.AccountException;
import shop.dodream.authservice.exception.AuthException;
import shop.dodream.authservice.jwt.JwtTokenProvider;
import shop.dodream.authservice.jwt.properties.JwtCookieProperties;
import shop.dodream.authservice.jwt.properties.JwtProperties;
import shop.dodream.authservice.repository.TokenRepository;


import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtCookieProperties jwtCookieProperties;
    private final TokenRepository tokenRepository;
    private final UserFeignClient userFeignClient;
    private final JwtProperties jwtProperties;

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
        );

        UserResponse user = userFeignClient.findByUserId(request.getUserId());
        if (user.getStatus() == Status.DORMANT) {
            throw new AccountException("휴면 계정입니다. 인증 후 로그인 해주세요.", Status.DORMANT, user.getUserId());
        } else if (user.getStatus() == Status.WITHDRAWN) {
            throw new AccountException("탈퇴된 계정입니다. 다른 아이디로 로그인 해주세요.", Status.WITHDRAWN, user.getUserId());
        }

        String uuid = UUID.randomUUID().toString();
        SessionUser sessionUser = new SessionUser(user.getUserId(), user.getRole());
        String accessToken = jwtTokenProvider.createAccessToken(uuid);
        String refreshToken = jwtTokenProvider.createRefreshToken(uuid);

        tokenRepository.save(uuid, sessionUser, refreshToken);
        userFeignClient.updateLastLogin(user.getUserId());
        return new TokenResponse(accessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("리프레시 토큰이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException("리프레시 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String uuid = jwtTokenProvider.getUuidFromToken(refreshToken);
        SessionUser sessionUser = tokenRepository.findByUuid(uuid);

        if (sessionUser == null) {
            throw new AuthException("세션이 만료되었습니다. 다시 로그인 해주세요.", HttpStatus.UNAUTHORIZED);
        }

        if (!tokenRepository.isValid(uuid, sessionUser, refreshToken)) {
            throw new AuthException("리프레시 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(uuid);
        return new TokenResponse(newAccessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),refreshToken);

    }

    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            String uuid = jwtTokenProvider.getUuidFromToken(refreshToken);
            tokenRepository.delete(uuid);
        }

        deleteTokenCookies(response);
    }

    private void deleteTokenCookies(HttpServletResponse response) {
        ResponseCookie accessTokenClear = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie refreshTokenClear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(jwtCookieProperties.isSecure())
                .sameSite(jwtCookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenClear.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenClear.toString());
    }
}