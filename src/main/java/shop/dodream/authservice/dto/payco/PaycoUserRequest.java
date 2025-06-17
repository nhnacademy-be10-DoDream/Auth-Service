package shop.dodream.authservice.dto.payco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaycoUserRequest {
    private String userId;
    private String password;
    private String email;
    private String name;
    private String phone;
    private Date birthDate;
}
