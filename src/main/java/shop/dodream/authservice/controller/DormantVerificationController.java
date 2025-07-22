package shop.dodream.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.dodream.authservice.client.DoorayMessageSender;
import shop.dodream.authservice.client.UserFeignClient;
import shop.dodream.authservice.dto.Status;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.service.DormantVerificationService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth/Dormant")
@RequestMapping("/auth/dormant")
public class DormantVerificationController {

    private final DormantVerificationService dormantService;
    private final DoorayMessageSender doorayMessageSender;
    private final UserFeignClient userFeignClient;

    @Operation(summary = "인증번호 전송",description = "휴면 계정의 인증 시 인증번호를 전송합니다.")
    @PostMapping("/request")
    public ResponseEntity<Void> sendVerificationCode(@RequestParam String userId){
        UserResponse user = userFeignClient.findByUserId(userId);
        if(user.getStatus() != Status.DORMANT){
            return ResponseEntity.badRequest().build();
        }
        dormantService.createAndSendCode(userId,doorayMessageSender);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "인증",description = "인증번호를 검증한 후 일치하면 사용자의 상태를 변경합니다.")
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String userId, @RequestParam String code){
        if(dormantService.verifyCode(userId,code)){
            userFeignClient.updateStatus(userId);
            return ResponseEntity.ok("휴면 상태가 해제되었습니다. 다시 로그인해주세요.");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증번호가 일치하지 않습니다. 다시 시도해주세요.");
    }


}
