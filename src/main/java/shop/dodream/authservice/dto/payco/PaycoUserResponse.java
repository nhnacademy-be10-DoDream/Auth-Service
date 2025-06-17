package shop.dodream.authservice.dto.payco;

import lombok.Data;

@Data
public class PaycoUserResponse {
    private PaycoUserHeader header;
    private PaycoUserData data;
}

