package shop.dodream.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import shop.dodream.authservice.dto.DoorayMessageRequest;

@FeignClient(name = "messageSendClient", url = "https://nhnacademy.dooray.com/services/3204376758577275363/4071284119244117501/RibHlHtpSlCOQ1Kesn_0KQ")
public interface DoorayMessageSender {

    @PostMapping
    void send(@RequestBody DoorayMessageRequest request);
}
