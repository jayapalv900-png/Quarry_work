package college.quarry;
public class ApiError extends RuntimeException {
 public final int status;
 public ApiError(int status,String message){super(message);this.status=status;}
 public static ApiError bad(String s){return new ApiError(400,s);}
}
