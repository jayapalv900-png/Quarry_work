package college.quarry;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static college.quarry.Values.*;
@RestController
public class ApiController {
 private final Database db;private final AuthService auth;private final QuarryService service;
 private static final Gson JSON=new GsonBuilder().serializeNulls().create();
 public ApiController(Database db,AuthService auth,QuarryService service){this.db=db;this.auth=auth;this.service=service;}
 @RequestMapping("/api/**")
 public ResponseEntity<String> handle(HttpServletRequest req){
  try{
   String method=req.getMethod(),path=req.getRequestURI(),authorization=req.getHeader("Authorization");
   if(!method.equals("GET")&&!method.equals("POST"))throw new ApiError(405,"Method not allowed");
   Map<String,Object> b=method.equals("POST")?body(req):new LinkedHashMap<>();
   if(method.equals("GET")){String id=req.getParameter("id");if(id!=null)b.put("id",id);}
   return db.transaction(c->{
    if(method.equals("POST")&&path.equals("/api/login"))return json(200,auth.login(c,b,req.getRemoteAddr()));
    if (method.equals("POST") && path.equals("/api/register")) {
    return json(
            201,
            auth.register(c, b, req.getRemoteAddr())
    );
}
    var u=auth.user(c,authorization);
    if(method.equals("POST")&&path.equals("/api/logout")){auth.logout(authorization);return json(200,msg("Logged out"));}
    if(method.equals("POST")&&path.equals("/api/password"))return json(200,auth.changePassword(c,u,b));
    if(method.equals("GET")&&path.equals("/api/reports")){
     String kind=req.getParameter("kind");if(kind==null)throw ApiError.bad("Choose a report");String csv=csv(service.report(c,kind,u));
     return ResponseEntity.ok().header("Content-Type","text/csv; charset=utf-8").header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=quarry-"+kind+".csv").body("\uFEFF"+csv);
    }
    Object result=service.route(c,method+" "+path,u,b);return json(200,result);
   });
  }catch(ApiError e){return json(e.status,obj("error",e.getMessage()));}
  catch(Exception e){e.printStackTrace();return json(500,obj("error","Internal server error. Check the server console."));}
 }
 private static Map<String,Object> body(HttpServletRequest req)throws Exception{
  byte[] data=req.getInputStream().readNBytes(65537);if(data.length>65536)throw new ApiError(413,"Request body exceeds 64 KiB");
  if(data.length==0)return new LinkedHashMap<>();String type=req.getContentType();if(type==null||!type.toLowerCase(Locale.ROOT).startsWith("application/json"))throw new ApiError(415,"Use application/json");
  try{JsonElement root=JsonParser.parseString(new String(data,StandardCharsets.UTF_8));if(!root.isJsonObject())throw ApiError.bad("Expected a JSON object");return JSON.fromJson(root,new TypeToken<Map<String,Object>>(){}.getType());}catch(JsonParseException|IllegalStateException e){throw ApiError.bad("Invalid JSON object");}
 }
 private static ResponseEntity<String> json(int status,Object value){return ResponseEntity.status(status).header("Content-Type","application/json; charset=utf-8").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").body(JSON.toJson(value));}
 private static String cell(Object v){String s=v==null?"":v.toString();if(v instanceof String&&s.stripLeading().matches("(?s)^[=+@-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
 static String csv(List<Map<String,Object>> rows){if(rows.isEmpty())return "No records\r\n";var keys=new ArrayList<>(rows.get(0).keySet());StringBuilder b=new StringBuilder();b.append(String.join(",",keys.stream().map(ApiController::cell).toList())).append("\r\n");for(var r:rows)b.append(String.join(",",keys.stream().map(k->cell(r.get(k))).toList())).append("\r\n");return b.toString();}
}
