package shop.dodream.authapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import shop.dodream.authapi.client.UserFeignClient;
import shop.dodream.authapi.dto.UserResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserFeignClient userFeignClient;
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserResponse user = userFeignClient.findByUserId(userId);
        if (user == null) {
            throw new UsernameNotFoundException("No user found");
        }
        return new User(
                user.getUserId(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()))
        );
    }
}
