package shop.dodream.authservice.dto;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionUser implements Serializable {
    private String userId;
    private Role role;
}
