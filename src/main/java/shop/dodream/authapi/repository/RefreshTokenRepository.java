package shop.dodream.authapi.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository {
    void save(String username, String refreshToken, long ttlMillis);
    boolean isValid(String username, String refreshToken);
    void delete(String username);
}
