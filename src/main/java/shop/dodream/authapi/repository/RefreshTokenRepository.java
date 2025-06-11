package shop.dodream.authapi.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository {
    void save(String userId, String refreshToken, long ttlMillis);
    boolean isValid(String userId, String refreshToken);
    void delete(String userId);
}
