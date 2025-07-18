package shop.dodream.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.Role;
import shop.dodream.authservice.dto.Status;
import shop.dodream.authservice.dto.UserResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserFeignClient userFeignClient;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameSuccess() {
        String userId = "test";
        String password = new BCryptPasswordEncoder().encode("password");
        UserResponse mockUserResponse = new UserResponse(userId,password,Role.USER,Status.ACTIVE);

        given(userFeignClient.findByUserId(userId)).willReturn(mockUserResponse);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId);

        assertThat(userDetails.getUsername()).isEqualTo(userId);
        assertThat(userDetails.getPassword()).isEqualTo(password);
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadByUsernameNotFound() {
        String userId = "test";
        given(userFeignClient.findByUserId(userId)).willReturn(null);

        assertThatThrownBy(()->
                customUserDetailsService.loadUserByUsername(userId))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("No user found");
    }
}
