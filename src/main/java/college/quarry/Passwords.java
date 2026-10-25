package college.quarry;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
public final class Passwords {
 private static final SecureRandom RANDOM=new SecureRandom();
 private Passwords(){}
 public static String token(int size){byte[] b=new byte[size];RANDOM.nextBytes(b);return HexFormat.of().formatHex(b);}
 public static String hash(String password,String salt)throws Exception{PBEKeySpec p=new PBEKeySpec(password.toCharArray(),HexFormat.of().parseHex(salt),210000,256);try{return HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(p).getEncoded());}finally{p.clearPassword();}}
 public static boolean matches(String password,String salt,String expected)throws Exception{return MessageDigest.isEqual(hash(password,salt).getBytes(StandardCharsets.US_ASCII),expected.getBytes(StandardCharsets.US_ASCII));}
 public static void validate(String p){if(p==null||p.length()<8||p.length()>128)throw ApiError.bad("Password must have 8–128 characters");}
}
