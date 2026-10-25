package college.quarry;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.util.*;
import java.math.BigDecimal;
import static college.quarry.Database.*;
import static college.quarry.Values.*;
@Service
public class QuarryService {
 public static final String STOCK="SELECT id,name,qty_kg/1000.0 AS tons,reorder_kg/1000.0 AS reorder,price_cents/100.0 AS price,(qty_kg<=reorder_kg) AS low FROM stone";
 public static final String ORDERS="SELECT id,uid,stone_id,stone,qty_kg/1000.0 AS qty,amount_cents/100.0 AS amt,unit_cents/100.0 AS price,status,invoice,plate,weighed_kg/1000.0 AS weighed,created,dispatched_at,received_at,received_uid,CASE WHEN received_at IS NOT NULL THEN 'Received' WHEN status='Dispatched' THEN 'Dispatched' WHEN status='Paid' THEN 'Awaiting dispatch' WHEN status='Cancelled' THEN 'Cancelled' ELSE 'Not dispatched' END AS delivery_status FROM orders";
 public static final String PROD="SELECT p.id,p.stone,p.qty_kg/1000.0 AS tons,p.quality,p.uid,u.name AS \"by\",p.created FROM production p JOIN users u ON u.id=p.uid";
 public static final String INCIDENTS="SELECT i.id,u.name AS \"by\",i.uid,i.severity,i.details,i.resolved,i.created FROM incidents i JOIN users u ON u.id=i.uid";
 public Object route(Connection c,String key,Map<String,Object>u,Map<String,Object>b)throws Exception{
 switch(key){
 case "GET /api/me":return u;
 case "GET /api/stock":{allow(u,"Customer","Worker","Manager","Storekeeper","Administrator");return rows(c,STOCK+" ORDER BY id");}
 case "GET /api/dashboard":{
  allow(u,"Manager","Administrator");return obj("kpi",obj("Stock (t)",BigDecimal.valueOf(scalar(c,"SELECT SUM(qty_kg) FROM stone"),3),"Revenue (INR)",money(scalar(c,"SELECT SUM(amount_cents) FROM orders WHERE status IN ('Paid','Dispatched')")),"Orders",scalar(c,"SELECT COUNT(*) FROM orders"),"Produced (t)",BigDecimal.valueOf(scalar(c,"SELECT SUM(qty_kg) FROM production WHERE quality='Pass'"),3),"Open incidents",scalar(c,"SELECT COUNT(*) FROM incidents WHERE resolved=0"),"Machines not running",scalar(c,"SELECT COUNT(*) FROM machines WHERE status!='Running'")),"low",rows(c,STOCK+" WHERE qty_kg<=reorder_kg"),"recent",rows(c,ORDERS+" ORDER BY id DESC LIMIT 8"));
 }
 case "GET /api/orders":{
  allow(u,"Customer","Manager","Storekeeper","Administrator");return "Customer".equals(u.get("role"))?rows(c,ORDERS+" WHERE uid=? ORDER BY id DESC",u.get("id")):rows(c,ORDERS+" ORDER BY id DESC");
 }
 case "POST /api/orders":{
  allow(u,"Customer");long stone=id(b.get("stone_id")),q=kg(b.get("qty"));var s=first(c,"SELECT * FROM stone WHERE id=?",stone);
  if(s==null)throw ApiError.bad("Unknown stone type");if(number(s,"qty_kg")<q)throw ApiError.bad("Insufficient stock");
  long amount=amount(q,number(s,"price_cents"));String code=required(b,"card");boolean paid=code.length()>=4&&!code.endsWith("0000");
  update(c,"INSERT INTO orders(uid,stone_id,stone,qty_kg,unit_cents,amount_cents,status,created) VALUES(?,?,?,?,?,?,?,?)",u.get("id"),stone,s.get("name"),q,s.get("price_cents"),amount,paid?"Paid":"Payment Failed",now());long order=lastId(c);
  if(!paid)return obj("message","Simulated payment declined. Failed attempt recorded; stock unchanged.","paymentFailed",true,"orderId",order);
  if(update(c,"UPDATE stone SET qty_kg=qty_kg-? WHERE id=? AND qty_kg>=?",q,stone,q)!=1)throw ApiError.bad("Insufficient stock");String invoice="INV-"+(1000+order);update(c,"UPDATE orders SET invoice=? WHERE id=?",invoice,order);
  return obj("message","Order confirmed: "+invoice,"orderId",order,"invoice",invoice);
 }
  case "POST /api/orders/receive":{
  allow(u,"Customer");long order=id(b.get("id"));
  var o=first(c,"SELECT * FROM orders WHERE id=?",order);owned(u,o);
  if(!"Dispatched".equals(o.get("status")))throw ApiError.bad("Only dispatched orders can be confirmed as received");
  if(o.get("received_at")!=null)throw ApiError.bad("Receipt already confirmed");
  if(update(c,"UPDATE orders SET received_at=?,received_uid=? WHERE id=? AND uid=? AND status='Dispatched' AND received_at IS NULL",now(),u.get("id"),order,u.get("id"))!=1)throw ApiError.bad("Order changed; refresh and try again");
  return msg("Receipt confirmed by the customer. Thank you.");
 }
case "POST /api/orders/cancel":{
  allow(u,"Customer","Manager","Storekeeper");var o=first(c,"SELECT * FROM orders WHERE id=?",id(b.get("id")));owned(u,o);
  if(!"Paid".equals(o.get("status")))throw ApiError.bad("Only paid, undispatched orders can be cancelled");
  if(update(c,"UPDATE orders SET status='Cancelled' WHERE id=? AND status='Paid'",o.get("id"))!=1)throw ApiError.bad("Order state changed");
  update(c,"UPDATE stone SET qty_kg=qty_kg+? WHERE id=?",o.get("qty_kg"),o.get("stone_id"));return msg("Order cancelled and stock restored. No real refund processed.");
 }
 case "POST /api/stock/receive":{
  allow(u,"Manager","Storekeeper");long q=kg(b.get("tons")),stone=id(b.get("stone_id"));if(update(c,"UPDATE stone SET qty_kg=qty_kg+? WHERE id=?",q,stone)!=1)throw ApiError.bad("Unknown stone type");return msg("Inventory updated");
 }
 case "POST /api/dispatch":{
  allow(u,"Manager","Storekeeper");long w=kg(b.get("weighed"));String plate=required(b,"plate");var o=first(c,"SELECT * FROM orders WHERE id=?",id(b.get("order_id")));
  if(o==null||!"Paid".equals(o.get("status")))throw ApiError.bad("Select a paid order");if(BigDecimal.valueOf(w).compareTo(BigDecimal.valueOf(number(o,"qty_kg")).multiply(new BigDecimal("1.05")))>0)throw ApiError.bad("Load exceeds the demo 5% tolerance");
  if(update(c,"UPDATE orders SET status='Dispatched',plate=?,weighed_kg=?,dispatched_at=? WHERE id=? AND status='Paid'",plate,w,now(),o.get("id"))!=1)throw ApiError.bad("Order state changed");return msg("Vehicle dispatched");
 }
 case "GET /api/prod":{
  allow(u,"Worker","Manager");return "Worker".equals(u.get("role"))?rows(c,PROD+" WHERE p.uid=? ORDER BY p.id DESC",u.get("id")):rows(c,PROD+" ORDER BY p.id DESC");
 }
 case "POST /api/prod":{
  allow(u,"Worker");if(!Boolean.TRUE.equals(b.get("safe")))throw ApiError.bad("Confirm site safety or report an incident before recording production");
  long q=kg(b.get("tons"));String quality=choice(b,"quality","Pass","Fail");var s=first(c,"SELECT * FROM stone WHERE id=?",id(b.get("stone_id")));if(s==null)throw ApiError.bad("Unknown stone type");
  update(c,"INSERT INTO production(stone_id,stone,qty_kg,quality,uid,created) VALUES(?,?,?,?,?,?)",s.get("id"),s.get("name"),q,quality,u.get("id"),now());if(quality.equals("Pass"))update(c,"UPDATE stone SET qty_kg=qty_kg+? WHERE id=?",q,s.get("id"));return msg(quality.equals("Pass")?"Production saved; stock increased":"Rejected batch saved; stock unchanged");
 }
 case "GET /api/machines":allow(u,"Manager");return rows(c,"SELECT * FROM machines ORDER BY id");
 case "POST /api/machines/status":{
  allow(u,"Manager");String status=choice(b,"status","Running","Maintenance","Breakdown");if(update(c,"UPDATE machines SET status=? WHERE id=?",status,id(b.get("id")))!=1)throw ApiError.bad("Machine not found");return msg("Machine status updated");
 }
 case "GET /api/mlog":allow(u,"Manager");return rows(c,"SELECT m.id,e.name AS machine,m.issue,m.cost_cents/100.0 AS cost,m.created FROM maintenance m JOIN machines e ON e.id=m.machine_id ORDER BY m.id DESC");
 case "POST /api/mlog":{
  allow(u,"Manager");long machine=id(b.get("machine_id"));if(first(c,"SELECT id FROM machines WHERE id=?",machine)==null)throw ApiError.bad("Unknown machine");update(c,"INSERT INTO maintenance(machine_id,issue,cost_cents,created) VALUES(?,?,?,?)",machine,required(b,"issue"),cents(b.get("cost")),now());return msg("Maintenance recorded");
 }
 case "GET /api/incidents":{
  allow(u,"Worker","Manager");return "Worker".equals(u.get("role"))?rows(c,INCIDENTS+" WHERE i.uid=? ORDER BY i.id DESC",u.get("id")):rows(c,INCIDENTS+" ORDER BY i.id DESC");
 }
 case "POST /api/incidents":{
  allow(u,"Worker","Manager");String severity=choice(b,"severity","Low","Medium","High");update(c,"INSERT INTO incidents(uid,severity,details,created) VALUES(?,?,?,?)",u.get("id"),severity,required(b,"details"),now());return msg("Incident reported");
 }
 case "POST /api/incidents/resolve":{
  allow(u,"Manager");if(update(c,"UPDATE incidents SET resolved=1 WHERE id=? AND resolved=0",id(b.get("id")))!=1)throw ApiError.bad("Incident missing or already resolved");return msg("Incident resolved");
 }
 case "GET /api/attendance":{
  allow(u,"Worker","Manager","Administrator");String sql="SELECT a.id,a.uid,u.name AS \"by\",a.event,a.created FROM attendance a JOIN users u ON u.id=a.uid";
  return "Worker".equals(u.get("role"))?rows(c,sql+" WHERE a.uid=? ORDER BY a.id DESC LIMIT 100",u.get("id")):rows(c,sql+" ORDER BY a.id DESC LIMIT 500");
 }
 case "POST /api/attendance":{
  allow(u,"Worker");String event=choice(b,"event","Check-in","Check-out");var last=first(c,"SELECT event FROM attendance WHERE uid=? ORDER BY id DESC LIMIT 1",u.get("id"));
  if(last==null&&event.equals("Check-out"))throw ApiError.bad("Check in first");if(last!=null&&event.equals(last.get("event")))throw ApiError.bad("You already recorded "+event);
  update(c,"INSERT INTO attendance(uid,event,created) VALUES(?,?,?)",u.get("id"),event,now());return msg(event+" recorded");
 }
 case "GET /api/users":allow(u,"Administrator");return rows(c,"SELECT id,name,username,role FROM users ORDER BY id");
 case "POST /api/users":{
  allow(u,"Administrator");String name=required(b,"name"),username=required(b,"username"),password=required(b,"password"),role=choice(b,"role","Administrator","Manager","Storekeeper","Worker","Customer");
  if(!username.matches("[A-Za-z0-9_.-]{3,40}"))throw ApiError.bad("Username: use 3–40 letters, numbers, dots, underscores or hyphens");Passwords.validate(password);
  if(first(c,"SELECT id FROM users WHERE username=?",username)!=null)throw ApiError.bad("Username already exists");if(role.equals("Manager")&&first(c,"SELECT id FROM users WHERE role='Manager'")!=null)throw ApiError.bad("Only one Manager account is allowed");
  addUser(c,name,username,password,role);return msg("Individual account created");
 }
 case "POST /api/weighbridge/calculate":{
  allow(u,"Manager","Storekeeper");BigDecimal empty=decimal(b.get("emptyKg")),loaded=decimal(b.get("loadedKg"));if(empty.signum()<=0||loaded.compareTo(empty)<=0)throw ApiError.bad("Loaded weight must exceed a positive empty weight");BigDecimal net=loaded.subtract(empty);return obj("plate",required(b,"plate"),"netKg",net,"netTonnes",net.movePointLeft(3));
 }
 case "GET /api/invoice":{
  allow(u,"Customer","Manager","Storekeeper","Administrator");var o=first(c,"SELECT o.*,u.name AS customer FROM orders o JOIN users u ON u.id=o.uid WHERE o.id=?",id(b.get("id")));owned(u,o);if(o.get("invoice")==null)throw ApiError.bad("No invoice for a failed payment");return obj("invoice",o.get("invoice"),"orderId",o.get("id"),"customer",o.get("customer"),"stone",o.get("stone"),"qty",BigDecimal.valueOf(number(o,"qty_kg"),3),"price",money(number(o,"unit_cents")),"amount",money(number(o,"amount_cents")),"status",o.get("status"),"created",o.get("created"));
 }
 default:throw new ApiError(404,"API endpoint not found");
 }
 }
 private void owned(Map<String,Object>u,Map<String,Object>o){if(o==null||"Customer".equals(u.get("role"))&&number(u,"id")!=number(o,"uid"))throw new ApiError(404,"Order not found");}
 public List<Map<String,Object>> report(Connection c,String kind,Map<String,Object>u)throws Exception{
  allow(u,"Manager","Administrator");switch(kind){case "stock":return rows(c,STOCK+" ORDER BY id");case "production":return rows(c,PROD+" ORDER BY p.id DESC");case "orders":return rows(c,ORDERS+" ORDER BY id DESC");default:throw ApiError.bad("Choose stock, production or orders");}
 }
}
