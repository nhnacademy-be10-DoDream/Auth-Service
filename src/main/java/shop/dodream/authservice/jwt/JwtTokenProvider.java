package shop.dodream.authservice.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shop.dodream.authservice.dto.Role;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

    public String createAccessToken(String userId, Role role){
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId)
                .claim("role",role.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtProperties.getAccessTokenExpiration()))
                .signWith(SignatureAlgorithm.HS256,jwtProperties.getSecret().getBytes())
                .compact();
    }

    public String createRefreshToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret().getBytes())
                .compact();
    }


    public boolean validateToken(String token) {
        try{
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret().getBytes())
                    .parseClaimsJws(token);
            return true;
        }catch (JwtException  | IllegalArgumentException e){
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret().getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
