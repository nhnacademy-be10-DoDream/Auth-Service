package shop.dodream.authservice.dto.payco;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaycoUserInfo {
    private String idNo;
    private String email;
    private String mobile;
    private String maskedEmail;
    private String maskedMobile;
    private String name;
    @JsonProperty("birthdayMMdd")
    private String birthday;
}
