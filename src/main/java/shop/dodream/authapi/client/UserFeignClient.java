package shop.dodream.authapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import shop.dodream.authapi.dto.UserResponse;

@FeignClient(name = "member-api")
public interface UserFeignClient {

    @GetMapping("/members/userId")
    UserResponse findByUserId(@RequestParam("userId")String userId);
}
