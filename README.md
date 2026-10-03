# Multi-Tenant Accounting Ledger Service

A RESTful backend application built using Java 17, Spring Boot, PostgreSQL, and Docker. The application provides double-entry journal accounting, tenant isolation, 
idempotent transaction creation, journal entry reversal, and account balance calculation.

For example if a company develops accounting software that is used by 100 different buisnesses. Each company uses 
same backend app to record its financial transactions.
This application:
1. Records financial transactions
2. Ensure every transaction is mathematically balanced
3. Get previous transactions
4. Reverse incorrect transactions without deleting history
5. Calculate account balances
6. Keep its data isolated from other buisnesses

## Features

* Create journal entries with debit and credit validation.
* Prevent duplicate journal entries using idempotency keys.
* Retrieve journal entries with pagination and optional date filters.
* Retrieve individual journal entries.
* Reverse existing journal entries without modifying the original.
* Calculate account balances, including historical balances.
* Isolate financial data using tenant identification.
* Persist data in PostgreSQL.
* Manage database schema using Flyway.
* Run the application and database together using Docker Compose

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Programming language |
| Spring Boot 3.5.6 | Backend framework |
| Spring Data JPA | Database access |
| PostgreSQL 16 | Relational database |
| Flyway | Database migrations |
| Maven | Build management |
| Docker | Containerization |
| JUnit 5 | Testing |
| MockMvc | API testing |

## Architecture

The application follows a layered architecture to separate HTTP handling, business logic, and database access.

Controller Layer
Receives HTTP requests, extracts request parameters and headers, invokes the appropriate service method, and returns HTTP responses.

Service Layer
Contains business logic such as:

* Validating journal entries.
* Checking debit-credit equality.
* Handling duplicate requests.
* Reversing journal entries.
* Calculating account balances.
* Applying tenant-specific operations.

Repository Layer
Uses Spring Data JPA to interact with PostgreSQL and retrieve or persist journal entries and their associated lines.

Database Layer
Stores journal entry headers, individual debit/credit lines, and related information. Flyway manages schema creation and migrations.

Request Flow
Client → Controller → Service → Repository → PostgreSQL

The response follows the reverse path back to the client.

## Prerequisites
- Docker Desktop installed and running
- Git

No local Java or PostgreSQL installation is required to run the application using Docker.

### 1. Clone the repository

git clone https://github.com/Dimple9812/Ledger-service.git
cd Ledger

Replace the repository URL with your actual GitHub repository URL.

### 2. Start the application

Run this single command from the project root:

docker compose up --build

Docker Compose automatically:

1. Starts PostgreSQL.
2. Waits for the database health check.
3. Builds the Spring Boot application.
4. Starts the Ledger API.
5. Applies database migrations through Flyway.

### Application URL
http://localhost:8080

### Stop the application
Press `Ctrl + C` in the terminal running Docker Compose.

Alternatively:
docker compose down

The PostgreSQL data volume is preserved when using this command.

## API Documentation
All endpoints use the base path:
/api/v1

Tenant Identification
Every ledger API request requires the following header:

X-Tenant-Id: tenant-A

* The tenant ID identifies which organization’s data the request is operating on.
* The application passes the tenant ID through the service layer and includes it in tenant-scoped database queries. 
  Database constraints also associate journal entries and their lines with the same tenant.
* This prevents a request from one tenant from retrieving or reversing another tenant’s journal
entry through the implemented API paths.

### API 1. Create Journal Entry

Endpoint 
POST /api/v1/journal-entries`

Purpose:
# Creates a new financial transaction in the ledger.
A journal entry represents a financial event and contains a date, description, and two or more transaction lines.

**Why this API is required**

* Financial transactions must be recorded accurately. The API ensures that an entry is validated before it is
persisted.
* It also handles retry scenarios. If a client sends a request but does not receive the response because of a 
network timeout, the client may retry. 
* The idempotency key helps prevent the same logical transaction from being recorded twice.

Request Headers

Content-Type: application/json
X-Tenant-Id: tenant-A

Example request:
{
  "entryDate": "2026-10-03",
  "description": "Office expense",
  "idempotencyKey": "txn-1001",
  "lines": [
    {
      "accountCode": "CASH",
      "amount": 1000,
      "direction": "DEBIT"
    },
    {
      "accountCode": "EXPENSE",
      "amount": 1000,
      "direction": "CREDIT"
    }
  ]
}
# The total debit amount must equal the total credit amount.

**How it works**

1. The controller receives the request and tenant ID.
2. The service validates the request and checks that at least two lines are present.
3. It checks that each amount is positive.
4. It calculates the total debit and total credit amounts.
5. It rejects the request if the two totals are not equal.
6. It checks whether the tenant has already used the supplied idempotency key.
7. If validation succeeds, it creates the journal entry and its associated lines.
8. The database uniqueness constraint on tenant ID and idempotency key provides an additional duplicate-prevention safeguard.
9. The saved entry is returned with its ID and posting timestamp.

* Design Decisions

1. Business validation is in the service layer: This keeps financial rules separate from HTTP handling and 
allows the same rules to be reused by other application flows.

2. BigDecimal for monetary values: Floating-point types such as double can introduce precision errors. 
BigDecimal is used for monetary calculations to preserve decimal accuracy.

3. Idempotency key: The key is included in the request body in this implementation. 
It is scoped to the tenant so that different tenants can use the same key independently.

4. Database uniqueness constraint: An application-level existence check alone is not sufficient when concurrent
requests arrive. A database uniqueness constraint provides an additional protection against duplicate records. 
Concurrent conflict handling should also return a controlled API response.

Expected Response
{
"createdAt": "2026-10-03T14:47:15.943030100Z",
"description": "Valid balanced transaction",
"entryDate": "2026-10-03",
"id": "generated-UUID",
"idempotencyKey": "payment-1008",
"postedAt": "2026-10-03T14:47:15.858496300Z",
"reversesEntryId": null,
"tenantId": "tenant-X"
}

Success:201 Created

Possible errors:
Invalid request, unbalanced entry, duplicate idempotency key

### 2. List Journal Entries
Endpoint
GET  /api/v1/journal-entries`

Purpose:
Retrieves journal entries belonging to the requesting tenant

**Why this API is required**

Users need to view and review previously recorded transactions. Returning every record at once can 
be inefficient as the ledger grows, so this API supports pagination and optional date filtering
Supported query parameters:

Example:

GET /api/v1/journal-entries?from=2026-10-01&to=2026-10-31&page=0&size=20

Request Headers
X-Tenant-Id: tenant-A

Query Parameters

from    Optional starting date
to      Optional ending date
page    Zero-based page number
size    number of records per page

**How it works**

1. The controller receives the tenant ID and query parameters.
2. The service validates the page number, page size, and date range.
3. It creates a pagination request using Spring Data’s PageRequest.
4. The repository retrieves only entries belonging to the specified tenant.
5. If dates are provided, the query filters entries by the date range.
6. The paginated result is returned to the client.

**Design Decisions**
1. Pagination: Prevents unnecessarily loading large numbers of records into memory and allows clients 
to retrieve data in manageable batches.
2. Optional date filters: Support transaction history searches for a particular period without
requiring separate endpoints.
3. Tenant-scoped query: Ensures that the list contains only the requesting tenant’s entries.

**Expected Response**
{
"createdAt": "2026-10-03T14:58:23.065122Z",
"description": "Reversal of journal entry: ec27a34a-4bc0-45e4-aabc-393bc69c9fcd",
"entryDate": "2026-10-03",
"id": "UUID",
"idempotencyKey": "reversal-ec27a34a-4bc0-45e4-aabc-393bc69c9fcd",
"postedAt": "2026-10-03T14:58:23.044730Z",
"reversesEntryId": "ec27a34a-4bc0-45e4-aabc-393bc69c9fcd",
"tenantId": "tenant-A"
},
.
.
.
.
.
{
"createdAt": "2026-10-03T12:20:09.024199Z",
"description": "Payment received from customer",
"entryDate": "2026-10-03",
"id": "a6988570-2471-4b70-8a40-bf02254e294f",
"idempotencyKey": "payment-1001",
"postedAt": "2026-10-03T12:20:08.965306Z",
"reversesEntryId": null,
"tenantId": "tenant-A"
}
],
"empty": false,
"first": true,
"last": true,
"number": 0,
"numberOfElements": 7,
"pageable": {
"offset": 0,
"pageNumber": 0,
"pageSize": 20,
"paged": true,
"sort": {
"empty": true,
"sorted": false,
"unsorted": true
},
"unpaged": false
},
"size": 20,
"sort": {
"empty": true,
"sorted": false,
"unsorted": true
},
"totalElements": 7,
"totalPages": 1
}
Status code:
Success: 200 OK


### 3. Get Journal Entry
EndPoint:
GET /api/v1/journal-entries/{id}`

Request Headers
X-Tenant-Id: tenant-A

**Purpose**
Retrieves the details of one journal entry using its unique ID.

**Why this API is required**
1. A user may need to inspect a specific transaction, including its description, date, posting
information, and debit/credit lines.
2. Retrieves a journal entry belonging to the requesting tenant.

**How it works**

1. The controller receives the entry ID and tenant ID.
2. The service searches for the entry using both identifiers.
3. If the entry belongs to the tenant, its details are returned.
4. If the entry does not exist for that tenant, the API returns 404 Not Found.

**Design Decision: Tenant-scoped lookup

1. The application does not look up an entry using its ID alone.
2. Including the tenant ID in the lookup prevents a tenant from accessing another tenant’s entry
merely by knowing its UUID.
3. Returning 404 Not Found for an entry outside the tenant’s scope also avoids confirming that another
tenant’s record exists.

Expected Response:
{
"createdAt": "2026-10-03T14:58:23.065122Z",
"description": "Reversal of journal entry: UUID",
"entryDate": "2026-10-03",
"id": "46a40420-1e86-4e79-a2f3-1e0846847994",
"idempotencyKey": "UUID",
"postedAt": "2026-10-03T14:58:23.044730Z",
"reversesEntryId": "ec27a34a-4bc0-45e4-aabc-393bc69c9fcd",
"tenantId": "tenant-A"
}
Success: 200 OK
Not found: 404 Not Found

### 4. Reverse Journal Entry

POST /api/v1/journal-entries/{id}/reverse`

Request Headers
X-Tenant-Id: tenant-A

**Purpose**
Creates a reversal entry with debit and credit directions swapped. The original entry remains unchanged.

**Why this API is required**
1. Accounting records should not be edited or deleted after posting because doing so removes the 
history of what happened.
2. If a transaction was entered incorrectly,
a reversing entry provides a traceable correction while preserving the original record.

**How it works**

1. The service retrieves the original entry using its ID and tenant ID.
2. It checks whether the entry has already been reversed.
3. If a reversal already exists, the request is rejected.
4. Otherwise, the service creates a new journal entry.
5. Every original debit becomes a credit, and every original credit becomes a debit.
6. The new entry is linked to the original using the reversal reference.
7. The original entry remains unchanged.
8. Both entries remain available for future retrieval and balance calculations.

Example

Original Entry:

Account    Debit    Credit
CASH       5,000     0
REVENUE    0         5.000

Reversal Entry

Account    Debit    Credit
CASH       0         5,000  
REVENUE    5,000         0

The combined financial effect is zero.

**Design Decisions**

1. Create a new entry instead of updating the original: Preserves an audit trail and follows the assignment’s 
immutable-ledger requirement.
2. Prevent duplicate reversals: A journal entry should not be reversed twice. The service checks for an 
existing reversal,and a database uniqueness constraint on the reversal reference provides an additional 
safeguard. 
3. Transactional processing: The reversal header and its lines should be saved as one transaction so that a partial reversal is not committed.

Success: 201 Created
Already reversed: 409 Conflict


### 5. Get Account Balance

GET /api/v1/accounts/{accountCode}/balance`

Request Headers
X-Tenant-Id: tenant-A

**Purpose**
Calculates the balance of a particular account for a tenant.

**Why this API is required**

Users need to know the current or historical financial position of an account without manually adding
every debit and credit transaction.

Example:
GET /api/v1/accounts/CASH/balance?asOf=2026-10-03

Query param:
asOf      Optional date up to which balance should be calculated
The response includes:
- Account code
- Total debits
- Total credits
- Net debit balance

Net debit balance = Total debits − Total credits.
If asOf is omitted, the calculation uses all matching entries.

**How it works**

1. AccountController receives the request.
2. It extracts the tenant ID, account code, and optional date.
3. It delegates the calculation to JournalEntryService.
4. The service retrieves journal entry lines matching the tenant and account.
5. If an asOf date is supplied, only lines associated with entries up to that date are considered.
6. The service calculates total debits and total credits.
7. It calculates the net debit balance:
   Net Debit Balance = Total Debits - Total Credits
8. The result is returned as an AccountBalanceResponse.

Design Decision: Separate AccountController
1. Account balance retrieval is a distinct resource operation from creating or retrieving journal entries.
2. Keeping it in a separate AccountController makes the API structure easier to understand and maintain:
* JournalEntryController handles journal-entry operations.
* AccountController handles account-related operations.

The actual calculation remains in the service layer, rather than placing business logic inside
the controller

Expected response:
{
"accountCode": "CASH",
"totalDebits": 6000,
"totalCredits": 1000,
"netDebitBalance": 5000
}

Success: 200 OK

## Multi-Tenancy
The application identifies tenants through the X-Tenant-Id request header.

1. Tenant identity is passed from the controller to the service layer and used in repository queries.
2. The database schema also uses tenant-aware uniqueness and relationship constraints to associate
journal entries and their lines with the correct tenant.

This design helps prevent:
* Reading another tenant’s journal entry.
* Reversing another tenant’s entry.
* Including another tenant’s transactions in an account balance.
* Associating a journal entry line with an entry belonging to a different tenant.

**Why this approach?**
1. Tenant filtering only in controller code can be forgotten when new repository methods are introduced.
2. Using tenant-aware database constraints provides an additional layer of protection. However, the 
application must still ensure that every data-access path includes the tenant scope. 
Database constraints do not automatically secure every possible query.
3. A tenant-isolation integration test verifies that tenant B cannot retrieve or reverse an entry created 
by tenant A, and that tenant B’s account balance is unaffected.

## Database
journal_entries -Stores the main journal entry information:
* Entry ID
* Tenant ID
* Entry date
* Description
* Idempotency key
* Posting timestamp
* Reversal reference
* Creation timestamp

journal_entry_lines - Stores individual debit and credit lines associated with a journal entry:

* Line ID
* Tenant ID
* Entry ID
* Account code
* Amount
* Direction (DEBIT or CREDIT)
A journal entry can contain multiple lines.

## Testing
Automated tests are written using JUnit 5, Spring Boot Test, and MockMvc.

The tenant-isolation test verifies that:

* Tenant A can retrieve its own entry.
* Tenant B receives 404 Not Found when requesting tenant A’s entry.
* Tenant B cannot reverse tenant A’s entry.
* Tenant B’s account balance is not affected by tenant A’s entry.

Additional API scenarios can be tested using Postman, including balanced and 
unbalanced entries, duplicate idempotency keys, reversal behavior, and historical account balances.

## Running Tests

To execute the automated test suite locally:
./mvnw test

On Windows PowerShell:
.\mvnw.cmd test

The project includes an automated tenant isolation integration test using Spring Boot Test and MockMvc.

## Project Structure

```text
Ledger/
├── src/
│   ├── main/
│   │   ├── java/com/ledger_service/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── enums/
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```
**Event-Driven Architecture – Kafka Integration (Proposed)**

**Objective**
1. In a production accounting system, other services such as reporting and analytics may need to receive 
notifications whenever a journal entry is successfully posted.
2. Apache Kafka can be used as a message broker to communicate these events asynchronously, 
reducing direct coupling between the Ledger Service and downstream consumers.

**Proposed Event Flow**
1. When a journal entry is created, the Ledger Service generates a JOURNAL_ENTRY_POSTED event containing the
journal entry ID, tenant ID, and relevant transaction metadata.
2. The event is intended to be published to a Kafka topic named journal-entry-events.
3. A Reporting Service can consume this event to update financial dashboards or reporting data without blocking the original journal entry request.

**Consistency Challenge**
Publishing a Kafka message and committing a PostgreSQL transaction are two separate operations. If the database transaction rolls back after the message is published,
downstream services may process an event for a journal entry that was never committed.
Similarly, if the database commit succeeds but Kafka publication fails, downstream services may never receive the event.

Proposed Solution: Transactional Outbox Pattern
To address this problem, the design uses the Transactional Outbox Pattern.

1. The journal entry, journal lines, and corresponding outbox event are saved within the same PostgreSQL transaction.
2. If the database transaction fails, both the financial records and outbox event are rolled back.
3. A background publisher periodically reads unpublished events from the outbox table.
4. The publisher sends these events to Kafka.
5. After successful publication, the event is marked as published.
6. If Kafka is unavailable, the event remains pending and can be retried.

**Reliability and Duplicate Handling**

The publisher may publish an event more than once
if a failure occurs after Kafka accepts the message but before the database records successful publication.
Therefore, every event should have a unique event ID, and downstream consumers should process events idempotently.

**Benefits**
* Decouples ledger operations from reporting services.
* Supports asynchronous communication.
* Prevents committed journal entries from losing their corresponding event records.
* Allows retries when Kafka is temporarily unavailable.
* Supports future integration with additional consumers.

Implementation Status
This is a proposed production architecture. Kafka and the outbox publisher are not part of the current implemented API flow.

## Design Considerations

- Database transactions protect journal entry creation and reversal.
- Database constraints provide additional protection against duplicate records.
- `BigDecimal` is used for monetary values.
- Tenant IDs are included in data access operations.
- UTC is configured for consistent application timestamps.

## Future Enhancements

- Webhook processing and duplicate event handling
- Additional concurrency-focused idempotency tests
- Improved observability and structured logging
- API documentation using OpenAPI/Swagger
