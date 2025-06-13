package shop.dodream.authservice.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository {
    void save(String userId, String refreshToken);
    boolean isValid(String userId, String refreshToken);
    void delete(String userId);
}
