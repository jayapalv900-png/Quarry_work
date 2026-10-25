QUARRYWORKS 2.0 — COMPLETE REPLACEMENT PROJECT
================================================
Java 17+ | Maven 3.6.3+ | Spring Boot 4.1.2 | SQLite
Backend and tests: Java. Browser UI: HTML/CSS/JavaScript.
No Python, updater programs, Node or frontend installation needed to use this project.

START CLEAN — DO NOT MERGE INTO THE OLD PROJECT
1. Extract this ZIP into a NEW folder.
2. Open the inner quarryworks-improved folder containing pom.xml.
3. Check java -version and mvn --version.
4. In that folder build:
"C:\Tools\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd" clean package
   Or: mvn clean package
   Or double-click build.cmd (uses the Maven path from your earlier messages).
5. Wait for BUILD SUCCESS. Tests run during this build.
6. Start:
java -jar target\quarryworks-2.0.0.jar
   Or double-click start.cmd.
7. Open http://localhost:8000 and use Ctrl+F5.
   If an old server uses 8000:
java -jar target\quarryworks-2.0.0.jar --server.port=8004
   Then open http://localhost:8004
8. Stop with Ctrl+C.

FIRST-START DEMO ACCOUNTS — PASSWORDS HAVE CHANGED FROM THE OLD PROJECT
Administrator: admin     / Admin@123
Manager:       manager   / Manager@123
Storekeeper:   store     / Store@123
Customer 1:    customer  / Customer@123   (Anita)
Customer 2:    customer2 / Customer2@123  (Arun)
Worker 1:      worker    / Worker@123     (Ravi)
Worker 2:      worker2   / Worker2@123    (Kumar)
These credentials are intentionally NOT displayed on the login page.
Seeds run only when demo mode is enabled AND the users table is empty.
Change initial passwords under My account. All of that user's sessions then end.
Do not use demo passwords for a real deployment.

NEW DATABASE — NO OLD-DATABASE MIGRATION
Application database: data\quarry-v2.db (relative to the working directory).
The old quarry.db is NOT read, imported or overwritten.
DO NOT copy quarry.db over quarry-v2.db; schemas are different.
This version starts with new users, inventory and machines. Orders, revenue,
production, attendance and incident counts start empty/zero until you add data.
To choose a separate data folder:
java -jar target\quarryworks-2.0.0.jar --quarry.home=demo-data --server.port=8004
Run only one application process against each database.
QUARRY_HOME and QUARRY_DEMO environment variables are supported.
quarry.demo=false creates an empty schema but no accounts. It requires a separate
account bootstrap; keep demo=true for this college demonstration.

WHAT IS IMPROVED
- Backend separated into application, controller, service, auth, database,
  values/validation and password utilities.
- Fixed textarea creation in Maintenance and Safety forms.
- Familiar sidebar, login, cards and table layout; quarry illustration included.
- Only one Manager enforced by both validation and a SQLite unique partial index.
- Multiple independent Customer/Worker accounts; more can be added by Admin.
- Workers see their own production/incidents/attendance by user ID.
  Manager sees all production/incidents. Customers see only their own orders.
- Inventory is integer kilograms; API displays tonnes with up to 3 decimal places.
- Monetary storage is integer paise. Order calculation uses BigDecimal rounding.
- Explicit JDBC transactions plus one per-process DB lock.
- Orders reserve/deduct stock on simulated successful payment only.
- Failed payments are retained and return paymentFailed=true, with stock unchanged.
- Cancellation restores stock once. Dispatched orders cannot be cancelled.
- Positive net truck load and demo 5% upper dispatch tolerance.
- Attendance prevents repeated check-in/check-out and initial check-out.
- Password change, server logout, eight-hour sessions and basic login throttling.
- Password hashing PBKDF2-HMAC-SHA256 with random salt.
- CSV stock, production and order exports for Manager/Admin.
- Printable demo invoices for permitted order viewers, including owner checks.
- Searchable tables, dropdowns, estimated order total, clear empty/error states.
- Compact cards by default; Show card details reveals explanatory notes.
- Java/JUnit tests included. No patch files required.

SOURCE MAP
src/main/java/college/quarry/QuarryApplication.java — Spring Boot entry point
ApiController.java — HTTP/JSON, CSV responses and error boundaries
QuarryService.java — role validation and business operations
AuthService.java — login sessions, logout and password changes
Database.java — schema, demo seeds, queries and transaction boundary
Values.java — quantities, money, IDs, text and validation
Passwords.java — password hashing and token generation
ApiError.java — API errors
src/main/resources/static/index.html — page shell
styles.css — responsive interface
app.js — navigation, API calls, forms, tables and invoices
src/test/java/college/quarry/ — service and application context tests

JAVA TESTS
mvn test
18 test methods include context startup, monetary arithmetic, payment failure,
order stock changes, one-time cancellation, ownership checks, dispatch, quality,
safety, worker isolation, single-manager rule, duplicate accounts, attendance,
maintenance validation, rollback, revenue, login/logout/password and reports.
Tests use temporary databases, not your application data.
Tests have NOT been executed here because the generation environment has no JDK/Maven.
Do not skip failing tests for your submission: report the first build error.

MANUAL DEMONSTRATION
1. Manager: dashboard starts at zero revenue with seeded stock and low-stock alert.
2. Worker: add 10 t Pass, safe=true; own production list and stock change.
3. Worker2: own production list remains empty; incident/attendance isolation too.
4. Customer: order 5 t code 1234; confirm invoice and stock deduction.
5. Customer2: cannot see Customer1's order.
6. Customer: code 0000 fails; order history retains failure; stock unchanged.
7. Customer: cancel paid order; stock restored exactly once.
8. Make another order; Storekeeper dispatches it. Later cancellation must fail.
9. Worker: incident description field works. Manager resolves it.
10. Manager: maintenance description works; cost appears in history.
11. Worker: check in, reject repeated check in, then check out.
12. Manager/Admin: export CSV reports. Customer: print own invoice.
13. Any user: change password; old sessions end; new password works.
14. Restart: records remain, sessions end.

HONEST SCOPE
Local academic demo only, bound to 127.0.0.1 by default. Not production-ready.
Payments and refunds are simulated. Never enter real card data.
Invoices are demo sales documents, not GST/tax-compliant invoices.
Safety checkboxes are declarations, not verified site safety or machine control.
Weighing uses manual inputs; no physical scale and no persisted gross/tare ticket.
One stone type per order, no shopping cart or order-items table.
No email/SMS, password-reset flow, account deletion, sensor control or audit log.
Sessions/rate limiting are in memory; restarts clear them. Local IP throttle is
shared by all accounts using that IP; wait five minutes after ten failed logins.
All-time revenue = current Paid + Dispatched amounts, not actual collected money.
Report downloads use a UTF-8 CSV format; printable invoices use browser printing.
No automated schema migration for legacy databases.
No promise of an 8.5 grade; rating depends on working tests and assessment criteria.

VERIFICATION PERFORMED
See VERIFICATION.txt. POM XML and JavaScript syntax checked. Textarea form creation
checked in a minimal simulated DOM. SQLite schema, one-manager uniqueness and
negative-inventory constraints exercised. ZIP integrity checked.
No Java compilation, Maven/JUnit run or browser end-to-end test was possible here.
