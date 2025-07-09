package shop.dodream.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.LoginRequest;
import shop.dodream.authservice.dto.Role;
import shop.dodream.authservice.dto.Status;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.jwt.properties.JwtProperties;
import shop.dodream.authservice.jwt.JwtTokenProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private UserFeignClient userFeignClient;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void loginSuccess() throws Exception {

        String rawPassword = "1234";
        String encoded = new BCryptPasswordEncoder().encode(rawPassword);
        LoginRequest request = new LoginRequest("testuser", rawPassword);

        given(userFeignClient.findByUserId("testuser"))
                .willReturn(new UserResponse("testuser", encoded, Role.USER, Status.ACTIVE));


        given(jwtTokenProvider.createRefreshToken(any()))
                .willReturn("mocked.refresh.token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("mocked.access.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value((int)(jwtProperties.getAccessTokenExpiration() / 1000)))
                .andExpect(jsonPath("$.refresh_token").value("mocked.refresh.token"));
    }
}
