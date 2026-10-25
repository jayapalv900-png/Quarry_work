package college.quarry;
import java.util.*;
import java.math.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public final class Values {
 private Values(){}
 public static Map<String,Object> obj(Object... p){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<p.length;i+=2)m.put((String)p[i],p[i+1]);return m;}
 public static Map<String,Object> msg(String s){return obj("message",s);}
 public static String text(Map<String,Object>b,String key){Object v=b.get(key);if(v==null)return "";if(!(v instanceof String))throw ApiError.bad("Expected text: "+key);String s=((String)v).trim();if(s.length()>4000)throw ApiError.bad("Text too long: "+key);return s;}
 public static String required(Map<String,Object>b,String key){String s=text(b,key);if(s.isBlank())throw ApiError.bad("Missing field: "+key);return s;}
 public static BigDecimal decimal(Object v){try{if(v==null)throw ApiError.bad("Enter a number");BigDecimal n=new BigDecimal(v.toString());if(n.abs().compareTo(new BigDecimal("1000000000"))>0)throw ApiError.bad("Number is too large");return n;}catch(NumberFormatException e){throw ApiError.bad("Enter a finite number");}}
 public static long id(Object v){try{long n=new BigDecimal(String.valueOf(v)).longValueExact();if(n<=0)throw ApiError.bad("Invalid ID");return n;}catch(NumberFormatException|ArithmeticException e){throw ApiError.bad("Invalid ID");}}
 public static long kg(Object v){try{BigDecimal n=decimal(v);if(n.signum()<=0)throw ApiError.bad("Quantity must be positive");return n.movePointRight(3).longValueExact();}catch(ArithmeticException e){throw ApiError.bad("Use at most 3 decimal places for tonnes");}}
 public static long cents(Object v){try{BigDecimal n=decimal(v);if(n.signum()<0)throw ApiError.bad("Amount cannot be negative");return n.movePointRight(2).longValueExact();}catch(ArithmeticException e){throw ApiError.bad("Use at most 2 decimal places for money");}}
 public static BigDecimal money(long cents){return BigDecimal.valueOf(cents,2);}
 public static long amount(long kg,long priceCents){return BigDecimal.valueOf(kg).multiply(BigDecimal.valueOf(priceCents)).divide(BigDecimal.valueOf(1000),0,RoundingMode.HALF_UP).longValueExact();}
 public static String now(){return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);}
 public static long number(Map<String,Object>r,String key){return ((Number)r.get(key)).longValue();}
 public static void allow(Map<String,Object>u,String... roles){if(!Arrays.asList(roles).contains(u.get("role")))throw new ApiError(403,"Not allowed for your role");}
 public static String choice(Map<String,Object>b,String key,String... allowed){String v=required(b,key);if(!Arrays.asList(allowed).contains(v))throw ApiError.bad("Invalid "+key);return v;}
}
