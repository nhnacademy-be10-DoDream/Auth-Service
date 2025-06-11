package shop.dodream.authapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponse {

    private String userId;
    private String password;
    private Role role;
}
