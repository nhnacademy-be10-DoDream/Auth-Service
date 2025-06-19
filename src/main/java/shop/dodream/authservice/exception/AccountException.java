package shop.dodream.authservice.exception;

import lombok.Getter;
import shop.dodream.authservice.dto.Status;

@Getter
public class AccountException extends RuntimeException {
    private final Status status;
    private final String userId;
    public AccountException(String message,Status status, String userId) {
        super(message);
        this.status = status;
        this.userId = userId;
    }
}
