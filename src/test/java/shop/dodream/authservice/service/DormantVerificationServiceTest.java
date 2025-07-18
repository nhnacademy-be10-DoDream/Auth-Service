package shop.dodream.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import shop.dodream.authservice.client.DoorayMessageSender;
import shop.dodream.authservice.dto.DoorayMessageRequest;
import shop.dodream.authservice.util.VerificationCodeUtil;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DormantVerificationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private DoorayMessageSender sender;

    @Captor
    private ArgumentCaptor<DoorayMessageRequest> requestCaptor;


    private DormantVerificationService service;

    @BeforeEach
    public void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        service = new DormantVerificationService(redisTemplate);
    }


    @Test
    void sendCodeTest(){
        String userId = "test";
        String expectedCode = "ABCD1234";

        try(MockedStatic<VerificationCodeUtil> mockedStatic = mockStatic(VerificationCodeUtil.class)) {
            mockedStatic.when(VerificationCodeUtil::generateCode).thenReturn(expectedCode);
            String actualCode = service.createAndSendCode(userId,sender);

            assertThat(actualCode).isEqualTo(expectedCode);

            verify(valueOps).set(
                    eq("dormant:" +  userId),
                    eq(expectedCode),
                    eq(Duration.ofMinutes(5))
            );

            verify(sender).send(requestCaptor.capture());
            DoorayMessageRequest sent = requestCaptor.getValue();

            assertThat(sent.getBotName()).isEqualTo("관리자");
            assertThat(sent.getText()).contains("인증번호를 입력해주세요");
            List<DoorayMessageRequest.Attachment> attachments = sent.getAttachments();
            assertThat(attachments).hasSize(1);

            DoorayMessageRequest.Attachment att = attachments.get(0);
            assertThat(att.getTitle()).contains("인증번호 안내");
            assertThat(att.getText()).contains("["+expectedCode+"]");
            assertThat(att.getTitleLink()).endsWith("/auth/dormant/verify-form?userId="+userId);

        }
    }

    @Test
    void verifyCode_success_deletesKey_and_returnsTrue() {
        String userId = "user1";
        String key = "dormant:" + userId;
        String stored = "XYZ789";

        given(redisTemplate.opsForValue().get(key)).willReturn(stored);

        boolean result = service.verifyCode(userId, stored);

        assertThat(result).isTrue();
        verify(redisTemplate).delete(key);
    }

    @Test
    void verifyCode_mismatch_returnsFalse_and_noDelete() {
        String userId = "user1";
        String key = "dormant:" + userId;
        String stored = "XYZ789";

        given(redisTemplate.opsForValue().get(key)).willReturn(stored);

        boolean result = service.verifyCode(userId, "WRONG");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyCode_noStoredCode_returnsFalse_and_noDelete() {
        String userId = "user2";
        String key = "dormant:" + userId;

        given(redisTemplate.opsForValue().get(key)).willReturn(null);

        boolean result = service.verifyCode(userId, "anything");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }


}
