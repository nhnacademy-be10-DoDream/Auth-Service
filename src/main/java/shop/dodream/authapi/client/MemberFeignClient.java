package shop.dodream.authapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import shop.dodream.authapi.dto.MemberResponse;

@FeignClient(name = "member-api")
public interface MemberFeignClient {

    @GetMapping("/members/userId")
    MemberResponse findByUserId(@RequestParam("userId")String userId);
}
