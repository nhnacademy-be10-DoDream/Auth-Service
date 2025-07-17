package shop.dodream.authservice.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import shop.dodream.authservice.dto.RefreshTokenInfo;
import shop.dodream.authservice.dto.SessionUser;
import shop.dodream.authservice.repository.TokenRepository;

import java.time.Duration;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class TokenRepositoryImpl implements TokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "session:";
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private final ObjectMapper objectMapper;

    @Override
    public void save(String uuid, SessionUser sessionUser, String refreshToken, String userAgent, String ip) {
        redisTemplate.opsForValue().set(KEY_PREFIX + uuid, sessionUser, Duration.ofDays(1));
        RefreshTokenInfo tokenInfo = new RefreshTokenInfo(refreshToken, userAgent, ip);
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + uuid, tokenInfo, Duration.ofDays(1));
    }

    @Override
    public boolean isValid(String uuid, SessionUser sessionUser, String refreshToken, String userAgent, String ip) {
        Object sessionObj = redisTemplate.opsForValue().get(KEY_PREFIX + uuid);
        Object tokenObj = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + uuid);
        if (sessionObj == null || tokenObj == null) return false;

        try {
            SessionUser storedSession = objectMapper.convertValue(sessionObj, SessionUser.class);
            RefreshTokenInfo storedRefreshToken = objectMapper.convertValue(tokenObj, RefreshTokenInfo.class);
            return sessionUser.equals(storedSession)
                    && refreshToken.equals(storedRefreshToken.getToken())
                    && Objects.equals(ip, storedRefreshToken.getIp())
                    && Objects.equals(userAgent, storedRefreshToken.getUserAgent());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SessionUser findByUuid(String uuid) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + uuid);
        return objectMapper.convertValue(obj, SessionUser.class);
    }

    @Override
    public void delete(String uuid) {
        redisTemplate.delete(KEY_PREFIX + uuid);
        redisTemplate.delete(REFRESH_KEY_PREFIX + uuid);
    }
}
