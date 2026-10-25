package college.quarry;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import static college.quarry.Database.*;
import static college.quarry.Values.*;
@Service
public class AuthService {
 record Session(long uid,long expires){}
 record Attempts(int count,long until){}
 private final Map<String,Session> sessions=new ConcurrentHashMap<>();
 private final Map<String,Attempts> attempts=new ConcurrentHashMap<>();
 public Map<String,Object> login(Connection c,Map<String,Object>b,String client)throws Exception{
  long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->e.getValue().until<now);sessions.entrySet().removeIf(e->e.getValue().expires<now);
  Attempts a=attempts.get(client);if(a!=null&&a.count>=10)throw new ApiError(429,"Too many failed logins. Wait 5 minutes.");
  String username=required(b,"username");Object raw=b.get("password");String password=raw instanceof String?(String)raw:"";
  var r=first(c,"SELECT * FROM users WHERE username=?",username);
  if(r==null||!Passwords.matches(password,(String)r.get("salt"),(String)r.get("pw"))){attempts.put(client,new Attempts(a==null?1:a.count+1,a==null?now+300000:a.until));throw new ApiError(401,"Invalid username or password");}
  attempts.remove(client);String token=Passwords.token(32);sessions.put(token,new Session(number(r,"id"),now+8*60*60*1000));return obj("token",token,"user",safe(r));
 }
 private String normalize(String t){return t!=null&&t.startsWith("Bearer ")?t.substring(7):t;}
 public Map<String,Object> user(Connection c,String token)throws Exception{token=normalize(token);Session s=token==null?null:sessions.get(token);if(s==null||s.expires<System.currentTimeMillis()){if(token!=null)sessions.remove(token);throw new ApiError(401,"Please sign in again");}var r=first(c,"SELECT id,name,username,role FROM users WHERE id=?",s.uid);if(r==null)throw new ApiError(401,"Account not found");return r;}
 public void logout(String token){if(token!=null)sessions.remove(normalize(token));}
 public Object changePassword(Connection c,Map<String,Object>u,Map<String,Object>b)throws Exception{String old=required(b,"currentPassword"),next=required(b,"newPassword");Passwords.validate(next);var r=first(c,"SELECT * FROM users WHERE id=?",u.get("id"));if(!Passwords.matches(old,(String)r.get("salt"),(String)r.get("pw")))throw ApiError.bad("Current password is incorrect");String salt=Passwords.token(16);update(c,"UPDATE users SET salt=?,pw=? WHERE id=?",salt,Passwords.hash(next,salt),u.get("id"));long uid=number(u,"id");sessions.entrySet().removeIf(e->e.getValue().uid==uid);return msg("Password changed. Sign in again.");}
 public static Map<String,Object> safe(Map<String,Object>r){return obj("id",r.get("id"),"name",r.get("name"),"username",r.get("username"),"role",r.get("role"));}
}
