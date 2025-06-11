package shop.dodream.authapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import shop.dodream.authapi.dto.UserResponse;

@FeignClient(name = "user-api")
public interface UserFeignClient {

    @GetMapping("/users/userId")
    UserResponse findByUserId(@RequestParam("userId")String userId);
}
