package shop.dodream.authservice.dto.payco;

import lombok.Data;

@Data
public class PaycoUserHeader {
    private boolean isSuccessful;
    private int resultCode;
    private String resultMessage;
}
