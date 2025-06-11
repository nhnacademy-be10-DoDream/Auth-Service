package shop.dodream.authapi.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import shop.dodream.authapi.repository.RefreshTokenRepository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String username, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set("refresh:" + username, refreshToken, Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isValid(String username, String refreshToken) {
        String saved = redisTemplate.opsForValue().get("refresh:" + username);
        return saved != null && saved.equals(refreshToken);
    }

    @Override
    public void delete(String username) {
        redisTemplate.delete("refresh:"+username);
    }
}
