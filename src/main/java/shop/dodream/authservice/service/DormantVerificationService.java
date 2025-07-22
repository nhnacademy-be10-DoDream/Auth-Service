package shop.dodream.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import shop.dodream.authservice.client.DoorayMessageSender;
import shop.dodream.authservice.dto.DoorayMessageRequest;
import shop.dodream.authservice.util.VerificationCodeUtil;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DormantVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX= "dormant:";

    public String createAndSendCode(String userId, DoorayMessageSender sender){
        String code = VerificationCodeUtil.generateCode();
        redisTemplate.opsForValue().set(PREFIX + userId, code, Duration.ofMinutes(5));

        DoorayMessageRequest.Attachment attachment = new DoorayMessageRequest.Attachment();
        attachment.setTitle("🔐 인증번호 안내");
        attachment.setText("인증번호는 ["+ code + "] 입니다. \n5분 이내에 입력해주세요.");
        attachment.setTitleLink("https://dodream.shop");
        attachment.setBotIconImage("https://static.dooray.com/static_images/dooray-bot.png");
        attachment.setColor("yellow");

        DoorayMessageRequest request = new DoorayMessageRequest();
        request.setBotName("관리자");
        request.setText("📌 아래 인증번호를 입력해주세요.");
        request.setAttachments(List.of(attachment));

        sender.send(request);

        return code;

    }

    public boolean verifyCode(String userId, String inputCode){
        String key = PREFIX + userId;
        String storedCode = redisTemplate.opsForValue().get(key);
        if(storedCode != null && storedCode.equals(inputCode)){
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }


}
