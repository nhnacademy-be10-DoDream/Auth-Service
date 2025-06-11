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
import shop.dodream.authapi.client.MemberFeignClient;
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
    private final MemberFeignClient memberFeignClient;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        MemberResponse member = memberFeignClient.findByUsername(request.getUsername());

        String accessToken = jwtTokenProvider.createAccessToken(member.getUsername(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getUsername());

        return ResponseEntity.ok(new TokenResponse(
                accessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        if(!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());

        if(!refreshTokenRepository.isValid(username, request.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Role role = memberFeignClient.findByUsername(username).getRole();
        String newAccessToken = jwtTokenProvider.createAccessToken(username, role);

        return ResponseEntity.ok(new TokenResponse(
                newAccessToken,"Bearer",(int)(jwtProperties.getAccessTokenExpiration()/1000),null)
        );
    }
}
