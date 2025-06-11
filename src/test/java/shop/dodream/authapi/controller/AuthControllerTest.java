package shop.dodream.authapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import shop.dodream.authapi.client.MemberFeignClient;
import shop.dodream.authapi.dto.*;
import shop.dodream.authapi.jwt.JwtProperties;
import shop.dodream.authapi.jwt.JwtTokenProvider;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private MemberFeignClient memberFeignClient;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void loginSuccess() throws Exception {

        String rawPassword = "1234";
        String encoded = new BCryptPasswordEncoder().encode(rawPassword);
        LoginRequest request = new LoginRequest("testuser", rawPassword);

        given(memberFeignClient.findByUserId("testuser"))
                .willReturn(new MemberResponse("testuser", encoded, Role.USER));

        given(jwtTokenProvider.createAccessToken(any(), any()))
                .willReturn("mocked.access.token");

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

    @Test
    void refreshSuccess() throws Exception {
        String userId = "testuser";
        String refreshToken = "valid.token";

        ValueOperations<String, String> ops = mock(ValueOperations.class);
        given(stringRedisTemplate.opsForValue()).willReturn(ops);
        given(ops.get("refresh:" + userId)).willReturn(refreshToken);

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(memberFeignClient.findByUserId(userId)).willReturn(new MemberResponse(userId, "pw", Role.USER));
        given(jwtTokenProvider.createAccessToken(eq(userId), eq(Role.USER)))
                .willReturn("new.access.token");

        RefreshRequest request = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new.access.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value((int)(jwtProperties.getAccessTokenExpiration() / 1000)))
                .andExpect(jsonPath("$.refresh_token").doesNotExist());
    }

    @Test
    void refreshFail_invalidToken() throws Exception {
        RefreshRequest request = new RefreshRequest("invalid");

        given(jwtTokenProvider.validateToken("invalid")).willReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshFail_tokenMismatch() throws Exception {
        String refreshToken = "client.token";
        String userId = "testuser";

        ValueOperations<String, String> ops = mock(ValueOperations.class);
        given(stringRedisTemplate.opsForValue()).willReturn(ops);
        given(ops.get("refresh:" + userId)).willReturn("server.token");

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);

        RefreshRequest request = new RefreshRequest(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
