package shop.dodream.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import shop.dodream.authservice.dto.*;
import shop.dodream.authservice.service.AuthService;
import shop.dodream.authservice.service.PaycoOAuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final PaycoOAuthService paycoOAuthService;
    private final AuthService authService;


    @Operation(summary = "로그인",description = "로그인 시 JWT를 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Validated @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok().body(tokenResponse);
    }

    @Operation(summary = "토큰 재발급",description = "만료된 accessToken을 refreshToken을 사용하여 재발급 합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        TokenResponse tokenResponse = authService.refresh(refreshToken);
        return ResponseEntity.ok().body(tokenResponse);
    }

    @Operation(summary = "페이코 로그인 페이지",description = "페이코 로그인 페이지 url을 전달합니다.")
    @GetMapping("/payco/authorize")
    public ResponseEntity<String> getAuthorizationUrl(){
        String url = paycoOAuthService.buildAuthorizationUrl();
        return ResponseEntity.ok(url);
    }

    @Operation(summary = "페이코 로그인",description = "페이코 OAuth2로그인 시 JWT를 발급합니다.")
    @PostMapping("/payco/callback")
    public ResponseEntity<TokenResponse> handlePaycoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ){
        TokenResponse tokenResponse = paycoOAuthService.loginWithPayco(code, state);
        return ResponseEntity.ok().body(tokenResponse);
    }

    @Operation(summary = "로그아웃",description = "로그아웃 시 토큰을 제거 합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사용자 정보 전달",description = "프론트에서 관리자 접근을 위한 사용자 권한 및 아이디를 전달합니다.")
    @PostMapping("/auth/role")
    public ResponseEntity<SessionUser> getSessionUser(HttpServletRequest request) {
        SessionUser sessionUser = authService.getSessionUser(request);
        return ResponseEntity.ok().body(sessionUser);
    }


}
