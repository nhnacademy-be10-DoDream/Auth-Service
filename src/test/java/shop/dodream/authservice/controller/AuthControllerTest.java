package shop.dodream.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import shop.dodream.authservice.dto.LoginRequest;
import shop.dodream.authservice.dto.Role;
import shop.dodream.authservice.dto.SessionUser;
import shop.dodream.authservice.dto.TokenResponse;
import shop.dodream.authservice.service.AuthService;
import shop.dodream.authservice.service.PaycoOAuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private PaycoOAuthService paycoOAuthService;

    @BeforeEach
    void setUp() {
        // /auth/login
        given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .willReturn(new TokenResponse("AT", "Bearer", 3600, "RT"));

        // /auth/refresh
        given(authService.refresh(eq("RT"), any(HttpServletRequest.class)))
                .willReturn(new TokenResponse("newAT", "Bearer", 3600, "RT"));

        // /auth/role
        given(authService.getSessionUser(any(HttpServletRequest.class)))
                .willReturn(new SessionUser("user1", Role.USER));

        // /auth/payco/authorize
        given(paycoOAuthService.buildAuthorizationUrl())
                .willReturn("https://payco/auth");

        // /auth/payco/callback
        given(paycoOAuthService.loginWithPayco(eq("code"), eq("state"), any(HttpServletRequest.class)))
                .willReturn(new TokenResponse("payAT", "Bearer", 3600, "payRT"));
    }

    @Test
    void login() throws Exception {
        LoginRequest req = new LoginRequest("user1", "pass");
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("AT"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").value("RT"));
    }

    @Test
    void refresh() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "RT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("newAT"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").value("RT"));
    }

    @Test
    void logout() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "RT")))
                .andExpect(status().isNoContent());
    }

    @Test
    void paycoAuthorize() throws Exception {
        mockMvc.perform(get("/auth/payco/authorize"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://payco/auth"));
    }

    @Test
    void paycoCallback() throws Exception {
        mockMvc.perform(post("/auth/payco/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("payAT"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").value("payRT"));
    }

    @Test
    void role() throws Exception {
        mockMvc.perform(post("/auth/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}