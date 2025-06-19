package shop.dodream.authservice.util;

public class VerificationCodeUtil {
    public static String generateCode(){
        return String.valueOf((int)((Math.random() *900000)+100000));
    }
}
