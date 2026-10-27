# QuarryWorks — Stone Quarry Management System

**A role-based web application for managing quarry
production, stone inventory, customer orders, payments, dispatch,
machinery, safety, and attendance.**
[![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Backend-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
> **Project note:** This README documents the intended design and
> workflows. Verify every feature, dependency, environment variable, and
> test against the source code before describing it as implemented.
> Payment is a simulation unless the code explicitly integrates a real
> payment provider.

## Contents

-   [Project overview](#project-overview)
-   [User roles](#user-roles)
-   [Technology stack](#technology-stack)
-   [Architecture](#architecture)
-   [Functional requirements](#functional-requirements)
-   [UML and data flow diagrams](#uml-and-data-flow-diagrams)
    -   [1. Use case diagram](#1-use-case-diagram)
    -   [2. Class diagram](#2-class-diagram)
    -   [3. Sequence diagram --- place an
        order](#3-sequence-diagram--place-an-order)
    -   [4. Activity diagram --- production
        recording](#4-activity-diagram--production-recording)
    -   [5. Component diagram](#5-component-diagram)
    -   [6. Deployment diagram](#6-deployment-diagram)
    -   [7. Level 0 DFD --- context
        diagram](#7-level-0-dfd--context-diagram)
    -   [8. Level 1 DFD](#8-level-1-dfd)
-   [Logical database overview](#logical-database-overview)
-   [Run locally](#run-locally)
-   [Testing](#testing)
-   [Security and data integrity](#security-and-data-integrity)
-   [Limitations](#limitations)
-   [Suggested repository structure](#suggested-repository-structure)
-   [Future improvements](#future-improvements)

## Project overview

QuarryWorks centralizes the key day-to-day activities of a stone quarry.
It is intended to reduce paper-based record keeping and make stock,
production, orders, and operational status easier to track.

### Main capabilities

-   Customer registration, stock browsing, order placement, invoice
    details, and order-status tracking.
-   Production records with stone type, quantity, date, and quality
    outcome.
-   Inventory receiving, stock adjustments, and low-stock monitoring.
-   Simulated payment records and order totals.
-   Dispatch details, vehicle/load information, and customer receipt
    confirmation.
-   Machinery status, scheduled maintenance, and maintenance costs.
-   Safety incident reporting and resolution tracking.
-   Worker attendance records.
-   Role-based access and authorized summaries/CSV reports.

## User roles

  -----------------------------------------------------------------------
  Role                                Main responsibilities
  ----------------------------------- -----------------------------------
  **Customer**                        Register, browse available stone
                                      stock, place orders, view
                                      invoices/status, and confirm
                                      receipt after dispatch.

  **Worker**                          Record production, report safety
                                      incidents, and mark attendance.

  **Manager**                         Monitor operations, review reports,
                                      manage machinery and maintenance,
                                      and coordinate dispatch.

  **Storekeeper**                     Receive/update stock, review
                                      eligible orders, and record
                                      dispatch details.

  **Administrator**                   Create staff accounts, assign
                                      roles, and review system summaries.
  -----------------------------------------------------------------------

Customers register themselves. Staff accounts are created by the
Administrator. **Permissions and ownership checks must be enforced on
the server**, not only by hiding menu items in the browser. Follow the
actual source code if its role rules differ from this design.

## Technology stack

  -----------------------------------------------------------------------
  Area                                Technology
  ----------------------------------- -----------------------------------
  Backend                             Java 17, Spring Boot

  Frontend                            HTML, CSS, JavaScript

  Database                            SQLite through JDBC, as described
                                      for the current project

  Build                               Maven

  Testing                             JUnit, if configured in `pom.xml`
                                      and the test source

  Hosting                             Render was discussed; verify the
                                      actual deployment configuration
  -----------------------------------------------------------------------

## Architecture

```mermaid
flowchart LR
    U["Customer or staff"] --> B["Browser<br/>HTML · CSS · JavaScript"]
    B -->|"HTTP requests / JSON"| API["Spring Boot API<br/>ApiController"]
    API --> AUTH["AuthService<br/>Authentication"]
    API --> SVC["QuarryService<br/>Business rules"]
    AUTH --> HASH["Password hashing"]
    SVC --> VAL["Input validation"]
    AUTH --> DB["JDBC / transaction layer"]
    SVC --> DB
    DB --> SQL[("SQLite<br/>quarry-v2.db")]
    API -->|"Response / report"| B

    classDef client fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef api fill:#CCFBF1,stroke:#0F766E,color:#134E4A,stroke-width:2px
    classDef service fill:#EDE9FE,stroke:#7C3AED,color:#3B0764,stroke-width:2px
    classDef data fill:#FFEDD5,stroke:#EA580C,color:#7C2D12,stroke-width:2px
    class U,B client
    class API api
    class AUTH,SVC,HASH,VAL service
    class DB,SQL data
```

The browser sends requests to the Spring Boot application.
Authentication identifies the user, business services apply quarry
rules, and the JDBC/database layer reads or writes records. Use
transactions for workflows that must update an order and its stock
together. The exact classes and routes should be checked in the
repository.

## Functional requirements

These are intended requirements, not proof that every feature has passed
testing.

  -----------------------------------------------------------------------
  ID                                  Requirement
  ----------------------------------- -----------------------------------
  FR-01                               Allow customers to register and
                                      users to log in and log out.

  FR-02                               Allow a user to change their own
                                      password.

  FR-03                               Allow the Administrator to create
                                      staff accounts and assign roles.

  FR-04                               Enforce the project's one-Manager
                                      rule if it is part of the
                                      implemented specification.

  FR-05                               Check role permissions and record
                                      ownership on the server.

  FR-06                               Store stone details, quantity,
                                      price, and reorder level.

  FR-07                               Allow customers to view available
                                      stock.

  FR-08                               Allow authorized staff to receive
                                      stock.

  FR-09                               Record production and quality
                                      results. Accepted production
                                      increases available stock; rejected
                                      production must not increase
                                      sellable stock.

  FR-10                               Let customers place orders and
                                      check stock availability.

  FR-11                               Simulate payment and record the
                                      result.

  FR-12                               Create order/invoice details and
                                      deduct stock only for a
                                      successfully paid order, using a
                                      consistent transaction.

  FR-13                               Let authorized staff record
                                      dispatch and vehicle/load details.

  FR-14                               Allow only the owning customer to
                                      confirm receipt after dispatch.

  FR-15                               Record machinery status,
                                      maintenance details, and costs.

  FR-16                               Record safety incidents and
                                      authorized resolution updates.

  FR-17                               Record worker attendance.

  FR-18                               Provide authorized summaries and
                                      CSV reports.
  -----------------------------------------------------------------------

## UML and data flow diagrams

The diagrams below are embedded as Mermaid source. GitHub renders
supported Mermaid diagrams directly in a Markdown README. They are
conceptual design diagrams: update them when the source code or
permissions change.

### 1. Use Case Diagram

```mermaid
flowchart LR
    C["Customer"]
    W["Worker"]
    M["Quarry Manager"]
    S["Storekeeper"]
    A["Administrator"]

    subgraph Q["QuarryWorks System"]
      UC1(["Register / Log in"])
      UC2(["Browse Stock"])
      UC3(["Place Order"])
      UC4(["View Invoice and Order Status"])
      UC5(["Confirm Receipt"])
      UC6(["Record Production"])
      UC7(["Report Safety Incident"])
      UC8(["Mark Attendance"])
      UC9(["Update Inventory"])
      UC10(["Receive Stock"])
      UC11(["Record Dispatch"])
      UC12(["Manage Machinery and Maintenance"])
      UC13(["View Operational Reports"])
      UC14(["Create Staff Accounts and Roles"])
      UC15(["Review System Summary"])
    end

    C --> UC1
    C --> UC2
    C --> UC3
    C --> UC4
    C --> UC5
    W --> UC6
    W --> UC7
    W --> UC8
    S --> UC9
    S --> UC10
    S --> UC11
    M --> UC12
    M --> UC13
    M --> UC6
    M --> UC7
    A --> UC14
    A --> UC15

    classDef customer fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef worker fill:#D1FAE5,stroke:#059669,color:#064E3B,stroke-width:2px
    classDef manager fill:#EDE9FE,stroke:#7C3AED,color:#3B0764,stroke-width:2px
    classDef store fill:#FFEDD5,stroke:#EA580C,color:#7C2D12,stroke-width:2px
    classDef admin fill:#FCE7F3,stroke:#DB2777,color:#831843,stroke-width:2px
    class C customer
    class W worker
    class M manager
    class S store
    class A admin
```

### 2. Class Diagram

This is a domain-level model. Match the class names, fields, methods,
and multiplicities to the actual Java implementation before treating it
as a source-code class diagram.

```mermaid
classDiagram
    direction LR

    class User {
      +Long id
      +String name
      +String email
      +String passwordHash
      +Role role
      +login()
      +changePassword()
    }
    class Customer {
      +String phone
      +placeOrder()
      +viewOrderStatus()
      +confirmReceipt()
    }
    class Worker {
      +String phone
      +recordProduction()
      +reportIncident()
      +markAttendance()
    }
    class Quarry {
      +Long quarryId
      +String quarryName
      +String location
    }
    class StoneStock {
      +Long stockId
      +String stoneType
      +double availableQuantity
      +double unitPrice
      +double reorderLevel
      +checkAvailability()
      +updateStock()
    }
    class ProductionRecord {
      +Long productionId
      +LocalDate date
      +double quantity
      +String qualityStatus
      +recordProduction()
    }
    class Order {
      +Long orderId
      +LocalDateTime orderDate
      +String status
      +double totalAmount
      +calculateTotal()
      +generateInvoice()
    }
    class OrderItem {
      +Long itemId
      +String stoneType
      +double quantity
      +double unitPrice
      +getTotalPrice()
    }
    class Payment {
      +Long paymentId
      +double amount
      +String status
      +String reference
      +processPayment()
    }
    class Dispatch {
      +Long dispatchId
      +String vehicleNumber
      +double loadWeight
      +LocalDateTime dispatchedAt
    }
    class Machinery {
      +Long machineId
      +String name
      +String status
      +updateStatus()
      +scheduleMaintenance()
    }
    class MaintenanceRecord {
      +Long maintenanceId
      +LocalDate maintenanceDate
      +double cost
      +String notes
    }
    class SafetyIncident {
      +Long incidentId
      +LocalDate incidentDate
      +String description
      +String status
      +updateStatus()
    }
    class Attendance {
      +Long attendanceId
      +LocalDate date
      +String status
      +markAttendance()
    }

    User <|-- Customer
    User <|-- Worker
    Quarry "1" --> "0..*" Worker : employs
    Quarry "1" --> "0..*" StoneStock : stores
    Quarry "1" --> "0..*" ProductionRecord : records
    Worker "1" --> "0..*" ProductionRecord : records
    StoneStock "1" --> "0..*" ProductionRecord : produces
    Customer "1" --> "0..*" Order : places
    Order "1" *-- "1..*" OrderItem : contains
    Order "1" --> "0..1" Payment : has
    Order "1" --> "0..1" Dispatch : dispatched as
    StoneStock "1" --> "0..*" OrderItem : fulfils
    Machinery "1" --> "0..*" MaintenanceRecord : has
    Worker "1" --> "0..*" SafetyIncident : reports
    Worker "1" --> "0..*" Attendance : records

    classDef user fill:#DBEAFE,stroke:#2563EB,color:#172554
    classDef operations fill:#D1FAE5,stroke:#059669,color:#064E3B
    classDef order fill:#EDE9FE,stroke:#7C3AED,color:#3B0764
    classDef safety fill:#FFEDD5,stroke:#EA580C,color:#7C2D12
    class User,Customer,Worker user
    class Quarry,StoneStock,ProductionRecord,Attendance operations
    class Order,OrderItem,Payment,Dispatch order
    class Machinery,MaintenanceRecord,SafetyIncident safety
```

### 3. Sequence Diagram --- Place an Order

The payment step represents the project's simulated payment workflow,
not a real payment gateway.

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant UI as Web Browser
    participant API as Spring Boot API
    participant INV as Inventory Service
    participant PAY as Payment Service
    participant DB as SQLite Database

    C->>UI: Select stone and quantity
    UI->>API: Submit order details
    API->>API: Authenticate and validate request
    API->>INV: Check available stock
    INV->>DB: Read stock quantity and price
    DB-->>INV: Stock information
    INV-->>API: Availability result

    alt Stock available
        API->>API: Calculate total amount
        API->>PAY: Simulate payment
        alt Payment successful
            PAY-->>API: Payment success
            API->>DB: Save order and payment; deduct stock
            Note over API,DB: Commit related changes in one transaction
            DB-->>API: Saved order reference
            API-->>UI: Order confirmation and invoice
            UI-->>C: Display confirmation
        else Payment failed
            PAY-->>API: Payment failure
            API-->>UI: Failure message; do not finalize order
            UI-->>C: Show payment result
        end
    else Stock unavailable
        API-->>UI: Insufficient stock message
        UI-->>C: Show unavailable quantity
    end
```

### 4. Activity Diagram --- Production Recording

Accepted production is added to sellable stock. Rejected production is
retained as a rejected record and does not increase available stock.

```mermaid
flowchart TD
    START(("Start")) --> LOGIN["Log in"]
    LOGIN --> SELECT["Open production module"]
    SELECT --> INPUT["Enter stone type, quantity, date and quality details"]
    INPUT --> VALID{"Input valid?"}
    VALID -- "No" --> ERROR["Show validation error"]
    ERROR --> INPUT
    VALID -- "Yes" --> QUALITY{"Quality accepted?"}
    QUALITY -- "Yes" --> STOCK["Increase available stock"]
    STOCK --> SAVE["Save accepted production record"]
    QUALITY -- "No" --> REJECT["Save rejected batch record"]
    REJECT --> SAVE_REJECT["Keep rejected quantity out of sellable stock"]
    SAVE --> REPORT["Refresh production summary"]
    SAVE_REJECT --> REPORT
    REPORT --> END(("End"))

    classDef start fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef process fill:#CCFBF1,stroke:#0F766E,color:#134E4A,stroke-width:2px
    classDef decision fill:#FEF3C7,stroke:#D97706,color:#78350F,stroke-width:2px
    classDef reject fill:#FCE7F3,stroke:#DB2777,color:#831843,stroke-width:2px
    class START,END start
    class LOGIN,SELECT,INPUT,STOCK,SAVE,REPORT process
    class VALID,QUALITY decision
    class ERROR,REJECT,SAVE_REJECT reject
```

### 5. Component Diagram

```mermaid
flowchart TB
    subgraph CLIENT["Client Layer"]
      WEB["Web UI<br/>HTML · CSS · JavaScript"]
      MOBILE["Responsive mobile browser"]
    end

    subgraph APP["QuarryWorks — Spring Boot Application"]
      CTRL["Controllers<br/>HTTP / REST"]
      AUTH["Authentication and<br/>Authorization"]
      SVC["Business Services<br/>Orders · Production · Stock"]
      OPS["Operations Services<br/>Dispatch · Safety · Attendance"]
      REPORT["Reporting Service<br/>Summaries · CSV"]
      REPO["JDBC / Repository Layer"]
    end

    subgraph DATA["Persistence Layer"]
      DB[("SQLite Database<br/>quarry-v2.db")]
    end

    PAY["Payment Simulation"]
    WEB <--> CTRL
    MOBILE <--> CTRL
    CTRL --> AUTH
    CTRL --> SVC
    CTRL --> OPS
    CTRL --> REPORT
    SVC --> REPO
    OPS --> REPO
    REPORT --> REPO
    AUTH --> REPO
    REPO <--> DB
    SVC --> PAY

    classDef client fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef app fill:#CCFBF1,stroke:#0F766E,color:#134E4A,stroke-width:2px
    classDef service fill:#EDE9FE,stroke:#7C3AED,color:#3B0764,stroke-width:2px
    classDef data fill:#FFEDD5,stroke:#EA580C,color:#7C2D12,stroke-width:2px
    class WEB,MOBILE client
    class CTRL,AUTH,SVC,OPS,REPORT,REPO app
    class PAY service
    class DB data
```

### 6. Deployment Diagram

This shows a typical deployment arrangement for the documented stack.
Adjust it to match the real hosting setup. A hosted SQLite file is not
automatically durable across restarts or redeployments.

```mermaid
flowchart TB
    subgraph USERDEVICE["User Device"]
      BROWSER["Web Browser<br/>HTML · CSS · JavaScript"]
    end

    subgraph HOST["Application Host"]
      SPRING["Java 17 Runtime<br/>Spring Boot Application"]
      CTRL["Controllers"]
      SERVICE["Services and Access Control"]
      JDBC["JDBC Data Access"]
      SPRING --> CTRL --> SERVICE --> JDBC
    end

    subgraph STORAGE["Database Storage"]
      SQLITE[("SQLite File<br/>quarry-v2.db")]
    end

    BROWSER -->|"HTTPS in hosted deployment<br/>HTTP for local development"| SPRING
    JDBC <-->|"SQL / JDBC"| SQLITE

    classDef device fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef application fill:#D1FAE5,stroke:#059669,color:#064E3B,stroke-width:2px
    classDef storage fill:#EDE9FE,stroke:#7C3AED,color:#3B0764,stroke-width:2px
    class BROWSER device
    class SPRING,CTRL,SERVICE,JDBC application
    class SQLITE storage
```

### 7. Level 0 DFD --- Context Diagram

The Level 0 DFD treats QuarryWorks as one process and shows its external
actors and information exchanges. It intentionally does not show
internal database stores.

```mermaid
flowchart LR
    QM["Quarry Manager"]
    QW["Quarry Worker"]
    SK["Storekeeper"]
    C["Customer"]
    A["Administrator"]
    SYS(("0<br/>Stone Quarry<br/>Management System"))

    QM -->|"Production and safety data"| SYS
    SYS -->|"Production, stock and safety reports"| QM

    QW -->|"Production, attendance and incident updates"| SYS
    SYS -->|"Inventory status and task feedback"| QW

    SK -->|"Stock updates and dispatch details"| SYS
    SYS -->|"Stock status and order status"| SK

    C -->|"Registration, order and payment details"| SYS
    SYS -->|"Invoice, receipt and order status"| C

    A -->|"Staff accounts, roles and system settings"| SYS
    SYS -->|"System status and authorized summaries"| A

    classDef actor fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef system fill:#CCFBF1,stroke:#0F766E,color:#134E4A,stroke-width:3px
    class QM,QW,SK,C,A actor
    class SYS system
```

### 8. Level 1 DFD

This Level 1 DFD decomposes the system into major processes and data
stores. Data flows are conceptual and should be reconciled with the
implemented database schema.

```mermaid
flowchart LR
    C["Customer"]
    W["Quarry Worker"]
    SK["Storekeeper"]
    M["Quarry Manager"]
    A["Administrator"]

    P1(["1.0<br/>Manage Users<br/>and Roles"])
    P2(["2.0<br/>Manage Orders<br/>and Payments"])
    P3(["3.0<br/>Record Production"])
    P4(["4.0<br/>Manage Inventory"])
    P5(["5.0<br/>Manage Machinery<br/>and Safety"])
    P6(["6.0<br/>Generate Reports"])

    D1[("D1<br/>Users")]
    D2[("D2<br/>Orders and Payments")]
    D3[("D3<br/>Production Records")]
    D4[("D4<br/>Inventory")]
    D5[("D5<br/>Machinery and Safety")]

    A -->|"Create staff / assign roles"| P1
    P1 <-->|"Account records"| D1
    P1 -->|"System status"| A

    C -->|"Order and payment details"| P2
    P2 -->|"Invoice and order status"| C
    P2 <-->|"Order/payment records"| D2
    P2 <-->|"Stock availability / deductions"| D4

    W -->|"Production details and quality"| P3
    P3 -->|"Production records"| D3
    P3 -->|"Accepted output stock update"| P4
    P4 <-->|"Stock records and adjustments"| D4
    SK -->|"Stock receipt and dispatch updates"| P4
    P4 -->|"Stock status"| SK

    M -->|"Machine updates and safety review"| P5
    W -->|"Safety incident details"| P5
    P5 <-->|"Maintenance and incident records"| D5

    D2 -->|"Order/payment data"| P6
    D3 -->|"Production data"| P6
    D4 -->|"Inventory data"| P6
    D5 -->|"Machinery and safety data"| P6
    D1 -->|"User and role data"| P6
    P6 -->|"Authorized reports and summaries"| M
    P6 -->|"Authorized system summaries"| A

    classDef actor fill:#DBEAFE,stroke:#2563EB,color:#172554,stroke-width:2px
    classDef process fill:#CCFBF1,stroke:#0F766E,color:#134E4A,stroke-width:2px
    classDef store fill:#EDE9FE,stroke:#7C3AED,color:#3B0764,stroke-width:2px
    class C,W,SK,M,A actor
    class P1,P2,P3,P4,P5,P6 process
    class D1,D2,D3,D4,D5 store
```

## Logical database overview

The following are logical record groups, not a guaranteed listing of the
exact SQL schema.

  -----------------------------------------------------------------------
  Record group                        Example information
  ----------------------------------- -----------------------------------
  Users                               Name, login identifier, password
                                      hash, role

  Stone stock                         Stone type, available quantity,
                                      unit price, reorder level

  Orders and order items              Customer, items, quantities,
                                      prices, status, totals

  Payments                            Amount, simulated payment outcome,
                                      reference

  Production                          Worker, stone type, quantity, date,
                                      quality result

  Dispatch                            Order, vehicle, load/weight
                                      details, dispatch status and time

  Machinery and maintenance           Machine status, maintenance dates,
                                      notes, costs

  Safety incidents                    Reporter, incident details, status,
                                      resolution

  Attendance                          Worker, date, attendance status
  -----------------------------------------------------------------------

Consult the actual database implementation (for example, `Database.java`
if present) for table names, columns, indexes, and constraints.

## Run locally

### Prerequisites

-   Java 17, or the version configured in `pom.xml`.
-   Maven, or the Maven wrapper if the repository includes it.
-   The project's source code and configuration files.

### Build and test

Run these commands from the directory containing `pom.xml`:

``` bash
mvn clean test
mvn clean package
```

Run the executable JAR produced in `target/`. Replace the example
filename with the actual filename generated by Maven:

``` bash
java -jar target/<actual-jar-name>.jar
```

Open the configured local address, commonly `http://localhost:8080` when
the default Spring Boot port is used.

### Configuration and secrets

Check the application source and configuration files to confirm
supported environment variables and database paths. Do not assume that
variables mentioned in an older README are implemented.

-   Never commit real passwords, tokens, private keys, or secret `.env`
    files.
-   Use a non-production database for demonstrations and tests.
-   Changing a database path does not automatically migrate existing
    data.
-   Configure persistent storage or a managed database before hosting
    real records.

## Testing

Run configured automated tests with:

``` bash
mvn clean test
```

Suggested test cases include:

-   Invalid login and protected endpoints.
-   Role restrictions and customer order ownership.
-   Missing fields, invalid quantities, and invalid price inputs.
-   Insufficient stock and successful stock deductions.
-   Accepted versus rejected production.
-   Failed payment and prevention of unintended order finalization.
-   Dispatch and owner-only receipt confirmation.
-   Transaction rollback when an order or stock update fails.
-   Machinery maintenance and safety incident status changes.

These are recommended test areas, not a claim that tests already exist
or pass. Check the test source and Maven reports for evidence.

## Security and data integrity

-   Hash passwords with a suitable password-hashing mechanism; never
    store plain-text passwords.
-   Enforce role authorization and record ownership in server-side code.
-   Validate all user input on the server.
-   Use parameterized SQL statements to reduce SQL injection risk.
-   Use transactions for coupled order, payment-record, and inventory
    changes.
-   Avoid logging passwords, authentication tokens, or other secrets.
-   Restrict CSV reports to authorized users and avoid exporting
    unnecessary personal data.

## Limitations

-   Payment is a simulation unless a real provider integration exists in
    the code.
-   Manual weight entry is not automatic weighbridge integration.
-   Machinery records do not control physical quarry equipment.
-   Receipt confirmation is a system record, not independent proof of
    physical delivery.
-   SQLite can be suitable for a small local demonstration, but a hosted
    application needs deliberately configured durable storage. A free
    hosting filesystem may be ephemeral.
-   The diagrams describe the intended design and must be verified
    against the actual application.
-   No live deployment, performance benchmark, or passing test result is
    claimed by this README.

## Suggested repository structure

Adapt this example to the files actually present in your repository:

``` text
QuarryWorks/
├── README.md
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
└── docs/
    └── images/
```

The Mermaid diagrams are already embedded in this README, so separate
image files are not required for GitHub rendering. If you also add
exported diagram images, store them in `docs/images/` and link them with
relative paths such as
`![Diagram description](docs/images/diagram-name.png)`.

## Future improvements

Potential future work:

-   Replace local SQLite with a managed persistent database for hosted
    deployments.
-   Integrate a real payment provider with appropriate security
    controls.
-   Add notifications for low stock, overdue maintenance, and order
    status changes.
-   Add delivery evidence and weighbridge integration.
-   Add audit logs for sensitive administrative and stock operations.
-   Add role-specific dashboards and exportable reports.

## License

No license is specified here. Add a `LICENSE` file if you intend to
publish or allow reuse of the project.
