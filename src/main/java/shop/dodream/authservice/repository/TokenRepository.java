package shop.dodream.authservice.repository;

import org.springframework.stereotype.Repository;
import shop.dodream.authservice.dto.SessionUser;

@Repository
public interface TokenRepository {
    void save(String uuid, SessionUser sessionUser,String refreshToken);
    boolean isValid(String uuid, SessionUser sessionUser,String refreshToken);
    void delete(String uuid);
    SessionUser findByUuid(String uuid);
}
