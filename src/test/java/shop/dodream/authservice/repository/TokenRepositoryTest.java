package shop.dodream.authservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import shop.dodream.authservice.dto.Role;
import shop.dodream.authservice.dto.SessionUser;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class TokenRepositoryTest {

    @Autowired
    private TokenRepository tokenRepository;

    private final String uuid = "test-uuid";
    private final SessionUser sessionUser = new SessionUser("user1", Role.USER);
    private final String refreshToken = "test-refresh-token";
    private final String userAgent = "Mozilla/5.0";
    private final String ip = "127.0.0.1";

    @BeforeEach
    public void setUp() {
        tokenRepository.save(uuid,sessionUser,refreshToken,userAgent,ip);
    }

    @Test
    void tearDown(){
        tokenRepository.delete(uuid);
    }

    @Test
    void saveAndFindByUuid() {
        SessionUser result = tokenRepository.findByUuid(uuid);
        assertThat(result).isEqualTo(sessionUser);
    }

    @Test
    void isValid() {
        boolean valid = tokenRepository.isValid(uuid, sessionUser, refreshToken, userAgent, ip);
        assertThat(valid).isTrue();
    }

    @Test
    void isValidFailedOtherToken() {
        boolean valid = tokenRepository.isValid(uuid, sessionUser, "wrong-token", userAgent, ip);
        assertThat(valid).isFalse();
    }

    @Test
    void deleteByUuid() {
        tokenRepository.delete(uuid);
        assertThat(tokenRepository.findByUuid(uuid)).isNull();
    }

}
