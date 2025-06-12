package shop.dodream.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import shop.dodream.authservice.dto.UserResponse;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/accounts")
    UserResponse findByUserId(@RequestParam("userId")String userId);
}
