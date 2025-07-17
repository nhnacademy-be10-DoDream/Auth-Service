package shop.dodream.authservice.repository;

import org.springframework.stereotype.Repository;
import shop.dodream.authservice.dto.SessionUser;

@Repository
public interface TokenRepository {
    void save(String uuid, SessionUser sessionUser,String refreshToken,String userAgent, String ip);
    boolean isValid(String uuid, SessionUser sessionUser,String refreshToken,String userAgent, String ip);
    void delete(String uuid);
    SessionUser findByUuid(String uuid);
}
