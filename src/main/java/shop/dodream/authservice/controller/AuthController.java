package shop.dodream.authservice.controller;

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


    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Validated @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok().body(tokenResponse);
    }

    // access토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        TokenResponse tokenResponse = authService.refresh(refreshToken);
        return ResponseEntity.ok().body(tokenResponse);
    }

    // Payco 로그인 URL 리턴
    @GetMapping("/payco/authorize")
    public ResponseEntity<String> getAuthorizationUrl(){
        String url = paycoOAuthService.buildAuthorizationUrl();
        return ResponseEntity.ok(url);
    }

    // Payco Redirect 이후 code + state 전달 → JWT 발급
    @PostMapping("/payco/callback")
    public ResponseEntity<TokenResponse> handlePaycoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ){
        TokenResponse tokenResponse = paycoOAuthService.loginWithPayco(code, state);
        return ResponseEntity.ok().body(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ResponseEntity.noContent().build();
    }

}
