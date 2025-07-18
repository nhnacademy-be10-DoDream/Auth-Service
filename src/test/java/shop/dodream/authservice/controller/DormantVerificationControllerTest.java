package shop.dodream.authservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import shop.dodream.authservice.client.DoorayMessageSender;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.Role;
import shop.dodream.authservice.dto.Status;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.service.DormantVerificationService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
public class DormantVerificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserFeignClient userFeignClient;

    @MockBean
    private DormantVerificationService dormantVerificationService;

    @MockBean
    private DoorayMessageSender doorayMessageSender;

    private final String userId = "dormant-user";

    @BeforeEach
    void setup() {
        given(userFeignClient.findByUserId(userId))
                .willReturn(new UserResponse(userId, "encoded", Role.USER, Status.DORMANT));

        given(dormantVerificationService.createAndSendCode(userId, doorayMessageSender))
                .willReturn("123456");
    }

    @Test
    void sendDormantCode() throws Exception {
        mockMvc.perform(post("/auth/dormant/request")
                        .param("userId", userId))
                .andExpect(status().isOk());
    }

    @Test
    void failedSendCode() throws Exception {
        given(userFeignClient.findByUserId("active-user"))
                .willReturn(new UserResponse("active-user", "encoded", Role.USER, Status.ACTIVE));

        mockMvc.perform(post("/auth/dormant/request")
                        .param("userId", "active-user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyCode() throws Exception {
        given(dormantVerificationService.verifyCode(userId, "123456")).willReturn(true);
        doNothing().when(userFeignClient).updateStatus(userId);

        mockMvc.perform(post("/auth/dormant/verify")
                        .param("userId", userId)
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("휴면 상태가 해제되었습니다. 다시 로그인해주세요."));
    }

    @Test
    void failedVerifyCode() throws Exception {
        given(dormantVerificationService.verifyCode(userId, "wrong")).willReturn(false);

        mockMvc.perform(post("/auth/dormant/verify")
                        .param("userId", userId)
                        .param("code", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("인증번호가 일치하지 않습니다. 다시 시도해주세요."));
    }
}
