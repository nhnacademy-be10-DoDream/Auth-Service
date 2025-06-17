package shop.dodream.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.dto.payco.PaycoUserRequest;

@FeignClient(name = "user-test")
public interface UserFeignClient {

    @GetMapping("/accounts")
    UserResponse findByUserId(@RequestParam("userId")String userId);

    @PostMapping("/accounts")
    UserResponse createPaycoUser(@RequestBody PaycoUserRequest request);
}
