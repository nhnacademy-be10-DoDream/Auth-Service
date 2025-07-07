package shop.dodream.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import shop.dodream.authservice.dto.UserResponse;
import shop.dodream.authservice.dto.payco.PaycoUserRequest;

@FeignClient(name = "user",url = "s1.java21.net:10324")
public interface UserFeignClient {

    @GetMapping("/accounts/{user-id}")
    UserResponse findByUserId(@PathVariable("user-id") String userId);

    @PostMapping("/accounts")
    UserResponse createPaycoUser(@RequestBody PaycoUserRequest request);

    @PutMapping("/accounts/{user-id}/status")
    void updateStatus(@PathVariable("user-id") String userId);

    @PutMapping("/accounts/{user-id}/last-login")
    void updateLastLogin(@PathVariable("user-id") String userId);
}
