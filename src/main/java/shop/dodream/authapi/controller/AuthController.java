package shop.dodream.authapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dodream.authapi.client.UserFeignClient;
import shop.dodream.authapi.dto.*;
import shop.dodream.authapi.jwt.JwtProperties;
import shop.dodream.authapi.jwt.JwtTokenProvider;
import shop.dodream.authapi.repository.RefreshTokenRepository;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserFeignClient userFeignClient;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
        );

        UserResponse user = userFeignClient.findByUserId(request.getUserId());

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        return ResponseEntity.ok(new TokenResponse(
                accessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        if(!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());

        if(!refreshTokenRepository.isValid(userId, request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Role role = userFeignClient.findByUserId(userId).getRole();
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);

        return ResponseEntity.ok(new TokenResponse(
                newAccessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),null)
        );
    }
}
