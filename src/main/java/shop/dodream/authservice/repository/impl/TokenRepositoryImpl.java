package shop.dodream.authservice.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import shop.dodream.authservice.dto.SessionUser;
import shop.dodream.authservice.repository.TokenRepository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class TokenRepositoryImpl implements TokenRepository {
    private final RedisTemplate<String,Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "session:";
    private static final String REFRESH_KEY_PREFIX = "refresh:";

    @Override
    public void save(String uuid, SessionUser sessionUser,String refreshToken) {
        redisTemplate.opsForValue().set(KEY_PREFIX+uuid, sessionUser, Duration.ofDays(1));
        stringRedisTemplate.opsForValue().set(REFRESH_KEY_PREFIX+uuid, refreshToken, Duration.ofDays(1));
    }

    @Override
    public boolean isValid(String uuid, SessionUser sessionUser,String refreshToken) {
        Object storedSession = redisTemplate.opsForValue().get(KEY_PREFIX + uuid);
        String storedRefresh = stringRedisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + uuid);
        return sessionUser.equals(storedSession) && refreshToken.equals(storedRefresh);
    }

    @Override
    public SessionUser findByUuid(String uuid) {
        return (SessionUser) redisTemplate.opsForValue().get(KEY_PREFIX + uuid);
    }

    @Override
    public void delete(String uuid) {
        redisTemplate.delete(KEY_PREFIX + uuid);
        stringRedisTemplate.delete(REFRESH_KEY_PREFIX + uuid);
    }
}
