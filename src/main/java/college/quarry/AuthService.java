package college.quarry;

import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static college.quarry.Database.*;
import static college.quarry.Values.*;

@Service
public class AuthService {
    record Session(long uid, long expires) {}
    record Attempts(int count, long until) {}

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> registrationNext = new ConcurrentHashMap<>();

    public Map<String, Object> register(
            Connection c, Map<String, Object> b, String client
    ) throws Exception {
        long now = System.currentTimeMillis();
        registrationNext.entrySet().removeIf(e -> e.getValue() <= now);
        Long next = registrationNext.get(client);
        if (next != null && next > now) {
            throw new ApiError(429, "Wait one minute before trying registration again.");
        }
        registrationNext.put(client, now + 60_000L);

        if (b.containsKey("role") && !"Customer".equals(b.get("role"))) {
            throw new ApiError(403, "Public registration is for Customer accounts only.");
        }
        String name = required(b, "name");
        String username = required(b, "username");
        if (name.length() > 100) {
            throw ApiError.bad("Name must have at most 100 characters.");
        }
        if (!username.matches("[A-Za-z0-9_.-]{3,40}")) {
            throw ApiError.bad("Username: use 3–40 letters, numbers, dots, underscores or hyphens.");
        }
        Object rawPassword = b.get("password");
        Object rawConfirm = b.get("confirmPassword");
        if (!(rawPassword instanceof String) || !(rawConfirm instanceof String)) {
            throw ApiError.bad("Enter and confirm your password.");
        }
        String password = (String) rawPassword;
        Passwords.validate(password);
        if (!password.equals(rawConfirm)) {
            throw ApiError.bad("Passwords do not match.");
        }
        if (first(c, "SELECT id FROM users WHERE username=?", username) != null) {
            throw ApiError.bad("Username is unavailable. Choose another.");
        }
        addUser(c, name, username, password, "Customer");
        return msg("Account created successfully. You can now sign in.");
    }

    public Map<String, Object> login(
            Connection c, Map<String, Object> b, String client
    ) throws Exception {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue().until() < now);
        sessions.entrySet().removeIf(e -> e.getValue().expires() < now);
        Attempts a = attempts.get(client);
        if (a != null && a.count() >= 10) {
            throw new ApiError(429, "Too many failed logins. Wait 5 minutes.");
        }
        String username = required(b, "username");
        Object raw = b.get("password");
        String password = raw instanceof String ? (String) raw : "";
        var r = first(c, "SELECT * FROM users WHERE username=?", username);
        if (r == null || !Passwords.matches(
                password, (String) r.get("salt"), (String) r.get("pw"))) {
            attempts.put(client, new Attempts(
                    a == null ? 1 : a.count() + 1,
                    a == null ? now + 300_000L : a.until()
            ));
            throw new ApiError(401, "Invalid username or password");
        }
        attempts.remove(client);
        String token = Passwords.token(32);
        sessions.put(token, new Session(number(r, "id"), now + 28_800_000L));
        return obj("token", token, "user", safe(r));
    }

    private String normalize(String token) {
        return token != null && token.startsWith("Bearer ")
                ? token.substring(7) : token;
    }

    public Map<String, Object> user(Connection c, String token) throws Exception {
        token = normalize(token);
        Session s = token == null ? null : sessions.get(token);
        if (s == null || s.expires() < System.currentTimeMillis()) {
            if (token != null) sessions.remove(token);
            throw new ApiError(401, "Please sign in again");
        }
        var r = first(c,
                "SELECT id,name,username,role FROM users WHERE id=?", s.uid());
        if (r == null) throw new ApiError(401, "Account not found");
        return r;
    }

    public void logout(String token) {
        if (token != null) sessions.remove(normalize(token));
    }

    public Object changePassword(
            Connection c, Map<String, Object> user, Map<String, Object> b
    ) throws Exception {
        Object oldRaw = b.get("currentPassword");
        Object newRaw = b.get("newPassword");
        if (!(oldRaw instanceof String) || !(newRaw instanceof String)) {
            throw ApiError.bad("Enter your current and new passwords.");
        }
        String old = (String) oldRaw;
        String next = (String) newRaw;
        Passwords.validate(next);
        var r = first(c, "SELECT * FROM users WHERE id=?", user.get("id"));
        if (r == null || !Passwords.matches(
                old, (String) r.get("salt"), (String) r.get("pw"))) {
            throw ApiError.bad("Current password is incorrect");
        }
        String salt = Passwords.token(16);
        update(c, "UPDATE users SET salt=?,pw=? WHERE id=?",
                salt, Passwords.hash(next, salt), user.get("id"));
        long uid = number(user, "id");
        sessions.entrySet().removeIf(e -> e.getValue().uid() == uid);
        return msg("Password changed. Sign in again.");
    }

    public static Map<String, Object> safe(Map<String, Object> r) {
        return obj("id", r.get("id"), "name", r.get("name"),
                "username", r.get("username"), "role", r.get("role"));
    }
}
