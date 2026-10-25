package college.quarry;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import static college.quarry.Values.*;
@Component
public class Database {
 private final String url;
 public final Object lock=new Object();
 public Database(@Value("${quarry.home:./data}")String home,@Value("${quarry.demo:true}")boolean demo)throws Exception{
  Path folder=Path.of(home).toAbsolutePath().normalize();Files.createDirectories(folder);url="jdbc:sqlite:"+folder.resolve("quarry-v2.db");
  transaction(c -> {
    schema(c);

    boolean noUsers =
            first(c, "SELECT id FROM users LIMIT 1") == null;

    if (demo && noUsers) {
        seed(c);
    }

    if (!demo && noUsers) {
        bootstrapAdministrator(c);
    }

    if (!demo) {
        initializeDeploymentCatalogue(c);
    }

    return null;
});
 }
 @FunctionalInterface public interface Work<T>{T run(Connection c)throws Exception;}
 public <T>T transaction(Work<T> work)throws Exception{synchronized(lock){try(Connection c=DriverManager.getConnection(url)){
  try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");}
  c.setAutoCommit(false);try{T result=work.run(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}
 }}}
 public static PreparedStatement prepare(Connection c,String sql,Object...a)throws SQLException{PreparedStatement p=c.prepareStatement(sql);try{for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);return p;}catch(SQLException e){p.close();throw e;}}
 public static int update(Connection c,String sql,Object...a)throws SQLException{try(PreparedStatement p=prepare(c,sql,a)){return p.executeUpdate();}}
 public static List<Map<String,Object>> rows(Connection c,String sql,Object...a)throws SQLException{try(PreparedStatement p=prepare(c,sql,a);ResultSet rs=p.executeQuery()){var list=new ArrayList<Map<String,Object>>();int n=rs.getMetaData().getColumnCount();while(rs.next()){var row=new LinkedHashMap<String,Object>();for(int i=1;i<=n;i++)row.put(rs.getMetaData().getColumnLabel(i),rs.getObject(i));list.add(row);}return list;}}
 public static Map<String,Object> first(Connection c,String sql,Object...a)throws SQLException{var r=rows(c,sql,a);return r.isEmpty()?null:r.get(0);}
 public static long scalar(Connection c,String sql,Object...a)throws SQLException{var r=first(c,sql,a);Object v=r.values().iterator().next();return v==null?0:((Number)v).longValue();}
 public static long lastId(Connection c)throws SQLException{return scalar(c,"SELECT last_insert_rowid()");}
 public static void addUser(Connection c,String name,String username,String password,String role)throws Exception{String salt=Passwords.token(16);update(c,"INSERT INTO users(name,username,salt,pw,role) VALUES(?,?,?,?,?)",name,username,salt,Passwords.hash(password,salt),role);}
 private void schema(Connection c)throws Exception{
  String[] sql={
  "CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY,name TEXT NOT NULL,username TEXT NOT NULL COLLATE NOCASE UNIQUE,salt TEXT NOT NULL,pw TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('Administrator','Manager','Customer','Worker','Storekeeper')))",
  "CREATE UNIQUE INDEX IF NOT EXISTS one_manager ON users(role) WHERE role='Manager'",
  "CREATE TABLE IF NOT EXISTS stone(id INTEGER PRIMARY KEY,name TEXT NOT NULL UNIQUE,qty_kg INTEGER NOT NULL CHECK(qty_kg>=0),reorder_kg INTEGER NOT NULL CHECK(reorder_kg>=0),price_cents INTEGER NOT NULL CHECK(price_cents>=0))",
  "CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY,uid INTEGER NOT NULL REFERENCES users(id),stone_id INTEGER NOT NULL REFERENCES stone(id),stone TEXT NOT NULL,qty_kg INTEGER NOT NULL CHECK(qty_kg>0),unit_cents INTEGER NOT NULL,amount_cents INTEGER NOT NULL CHECK(amount_cents>=0),status TEXT NOT NULL CHECK(status IN ('Paid','Payment Failed','Cancelled','Dispatched')),invoice TEXT,plate TEXT,weighed_kg INTEGER,created TEXT NOT NULL)",
  "CREATE TABLE IF NOT EXISTS production(id INTEGER PRIMARY KEY,stone_id INTEGER REFERENCES stone(id),stone TEXT NOT NULL,qty_kg INTEGER NOT NULL CHECK(qty_kg>0),quality TEXT NOT NULL CHECK(quality IN ('Pass','Fail')),uid INTEGER REFERENCES users(id),created TEXT NOT NULL)",
  "CREATE TABLE IF NOT EXISTS machines(id INTEGER PRIMARY KEY,name TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('Running','Maintenance','Breakdown')))",
  "CREATE TABLE IF NOT EXISTS maintenance(id INTEGER PRIMARY KEY,machine_id INTEGER REFERENCES machines(id),issue TEXT NOT NULL,cost_cents INTEGER NOT NULL CHECK(cost_cents>=0),created TEXT NOT NULL)",
  "CREATE TABLE IF NOT EXISTS incidents(id INTEGER PRIMARY KEY,uid INTEGER REFERENCES users(id),severity TEXT NOT NULL CHECK(severity IN ('Low','Medium','High')),details TEXT NOT NULL,resolved INTEGER NOT NULL DEFAULT 0,created TEXT NOT NULL)",
  "CREATE TABLE IF NOT EXISTS attendance(id INTEGER PRIMARY KEY,uid INTEGER REFERENCES users(id),event TEXT NOT NULL CHECK(event IN ('Check-in','Check-out')),created TEXT NOT NULL)"};
    for(String s:sql)update(c,s);
  var columns=rows(c,"PRAGMA table_info(orders)");
  var names=new java.util.HashSet<String>();
  for(var column:columns)names.add(String.valueOf(column.get("name")));
  if(!names.contains("dispatched_at"))update(c,"ALTER TABLE orders ADD COLUMN dispatched_at TEXT");
  if(!names.contains("received_at"))update(c,"ALTER TABLE orders ADD COLUMN received_at TEXT");
  if(!names.contains("received_uid"))update(c,"ALTER TABLE orders ADD COLUMN received_uid INTEGER REFERENCES users(id)");

 }
private void bootstrapAdministrator(Connection c) throws Exception {

    String username = System.getenv("QUARRY_ADMIN_USERNAME");
    String password = System.getenv("QUARRY_ADMIN_PASSWORD");

    if (username == null && password == null) {
        System.err.println(
                "Administrator bootstrap variables are not set. "
                + "No initial account was created."
        );
        return;
    }

    if (username == null || password == null) {
        throw new IllegalStateException(
                "Set both QUARRY_ADMIN_USERNAME "
                + "and QUARRY_ADMIN_PASSWORD."
        );
    }

    username = username.trim();

    if (!username.matches("[A-Za-z0-9_.-]{3,40}")) {
        throw new IllegalStateException(
                "Invalid initial administrator username."
        );
    }

    Passwords.validate(password);

    addUser(
            c,
            "Administrator",
            username,
            password,
            "Administrator"
    );
}
private void initializeDeploymentCatalogue(Connection c)
        throws Exception {

    if (first(c, "SELECT id FROM stone LIMIT 1") == null) {

        update(
                c,
                """
                INSERT INTO stone
                    (name, qty_kg, reorder_kg, price_cents)
                VALUES (?, ?, ?, ?)
                """,
                "Granite 20mm", 0, 40000, 90000
        );

        update(
                c,
                """
                INSERT INTO stone
                    (name, qty_kg, reorder_kg, price_cents)
                VALUES (?, ?, ?, ?)
                """,
                "Granite 40mm", 0, 30000, 85000
        );

        update(
                c,
                """
                INSERT INTO stone
                    (name, qty_kg, reorder_kg, price_cents)
                VALUES (?, ?, ?, ?)
                """,
                "M-Sand", 0, 60000, 60000
        );

        update(
                c,
                """
                INSERT INTO stone
                    (name, qty_kg, reorder_kg, price_cents)
                VALUES (?, ?, ?, ?)
                """,
                "Rubble", 0, 30000, 40000
        );
    }

    if (first(c, "SELECT id FROM machines LIMIT 1") == null) {

        update(
                c,
                "INSERT INTO machines(name,status) VALUES(?,?)",
                "Excavator EX-1",
                "Maintenance"
        );

        update(
                c,
                "INSERT INTO machines(name,status) VALUES(?,?)",
                "Crusher JC-2",
                "Maintenance"
        );

        update(
                c,
                "INSERT INTO machines(name,status) VALUES(?,?)",
                "Drill DR-3",
                "Maintenance"
        );
    }
}
 private void seed(Connection c)throws Exception{
  addUser(c,"Administrator","admin","Admin@123","Administrator");addUser(c,"Meena","manager","Manager@123","Manager");addUser(c,"Suresh","store","Store@123","Storekeeper");
  addUser(c,"Anita","customer","Customer@123","Customer");addUser(c,"Arun","customer2","Customer2@123","Customer");addUser(c,"Ravi","worker","Worker@123","Worker");addUser(c,"Kumar","worker2","Worker2@123","Worker");
  update(c,"INSERT INTO stone(name,qty_kg,reorder_kg,price_cents) VALUES(?,?,?,?)","Granite 20mm",120000,40000,90000);
  update(c,"INSERT INTO stone(name,qty_kg,reorder_kg,price_cents) VALUES(?,?,?,?)","Granite 40mm",80000,30000,85000);
  update(c,"INSERT INTO stone(name,qty_kg,reorder_kg,price_cents) VALUES(?,?,?,?)","M-Sand",200000,60000,60000);
  update(c,"INSERT INTO stone(name,qty_kg,reorder_kg,price_cents) VALUES(?,?,?,?)","Rubble",25000,30000,40000);
  update(c,"INSERT INTO machines(name,status) VALUES(?,?)","Excavator EX-1","Running");update(c,"INSERT INTO machines(name,status) VALUES(?,?)","Crusher JC-2","Running");update(c,"INSERT INTO machines(name,status) VALUES(?,?)","Drill DR-3","Maintenance");
 }
}
