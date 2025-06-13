package shop.dodream.authservice.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import shop.dodream.authservice.repository.RefreshTokenRepository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String userId, String refreshToken) {
        redisTemplate.opsForValue().set("refresh:" + userId, refreshToken, Duration.ofDays(1));
    }

    @Override
    public boolean isValid(String userId, String refreshToken) {
        String saved = redisTemplate.opsForValue().get("refresh:" + userId);
        return saved != null && saved.equals(refreshToken);
    }

    @Override
    public void delete(String userId) {
        redisTemplate.delete("refresh:"+userId);
    }
}
