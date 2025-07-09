package shop.dodream.authservice.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import shop.dodream.authservice.dto.Status;
import shop.dodream.authservice.exception.AccountException;
import shop.dodream.authservice.exception.AuthException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<Map<String,Object>> handleDormantAccount(AccountException e){
        Map<String,Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("status", e.getStatus().name());
        body.put("userId", e.getUserId());

        HttpStatus httpStatus = e.getStatus() == Status.DORMANT ? HttpStatus.LOCKED : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(httpStatus).body(body);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthException e) {
        Map<String, String> body = Map.of("message", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(body);
    }


}
