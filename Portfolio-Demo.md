# Inventory Management REST API

## Spring Boot, JWT, PostgreSQL, Docker and Testcontainers

This portfolio project demonstrates a production-style inventory management backend for
handling users, role-based permissions, supplier purchase orders, partial stock receipts,
warehouse balances, database migrations, API documentation and automated integration tests.

## Project highlights

- Java 21 and Spring Boot REST API
- JWT authentication and role-based authorization
- PostgreSQL persistence with Flyway migrations
- Purchase-order workflow with controlled status transitions
- Warehouse-level inventory balances
- Request validation and RFC 7807 Problem Details responses
- Swagger/OpenAPI documentation
- Docker Compose deployment
- PostgreSQL integration testing with Testcontainers
- GitHub Actions continuous integration
- Immutable stock-receipt audit history

## Interactive API documentation

Swagger UI documents the available authentication, user administration, catalog,
purchase-order and stock endpoints. It can also be used to authorize with a JWT and execute
requests directly from the browser.

![Swagger UI showing the available APIs](docs/swagger%20ui%20apis.png)

## Containerized deployment

The API and PostgreSQL database can be started together with Docker Compose. Environment
variables are used for database credentials, JWT configuration and bootstrap administrator
settings.

![Docker image for the inventory management API](docs/Docker%20Image.png)

## Authentication and user roles

Public registration creates a user with the safe default `VIEWER` role. Passwords are
hashed, and successful login returns a signed JWT used for protected API requests.

![Registering a user with the Viewer role](docs/Register%20As%20a%20Viewer.png)

Administrators can list application users and assign the roles needed for purchasing and
inventory operations.

![Listing application users](docs/List%20Users.png)

![Changing a user's role](docs/Change%20User%20Role.png)

Authorization rules prevent a Viewer from performing a stock purchase or changing protected
business data.

![Viewer role rejected when attempting to purchase stock](docs/Purchase%20Stock%20with%20Viewer%20Role.png)

## Purchase-order workflow

Purchase orders use an explicit workflow instead of increasing stock immediately:

```text
DRAFT -> SUBMITTED -> APPROVED -> PARTIALLY_RECEIVED -> RECEIVED
  |          |            |
  +----------+------------+-> CANCELLED
```

Creating an order records the supplier, destination warehouse, products, ordered quantities
and unit costs. The initial status is `DRAFT`, and inventory remains unchanged.

![Successful supplier stock purchase API response](docs/Purchase%20stock.png)

![Creating a draft purchase order](docs/create%20a%20draft%20purchase%20order.png)

A purchasing agent submits the draft for review. Invalid state transitions are rejected by
the service layer.

![Submitting a draft purchase order](docs/Submit%20a%20draft%20purchase%20order.png)

An administrator or inventory manager approves the submitted order, providing separation
between purchasing and approval responsibilities.

![Approving a submitted purchase order](docs/approve%20submitted%20purchase%20order.png)

Approved products may be received in one or several deliveries. The API tracks ordered,
received and outstanding quantities, prevents over-receipt, and updates warehouse stock in
the same database transaction. Every delivery is retained with its product, quantity,
receiver and timestamp for audit purposes.

![Receiving stock against a purchase order](docs/receive%20purchase%20order.png)

A first delivery moves the order to `PARTIALLY_RECEIVED` while clearly showing the remaining
outstanding quantity.

![Partially received purchase order](docs/partially%20received%20order.png)

The receipt-history endpoint provides an auditable record of each delivery, including the
received product, quantity, user and timestamp.

![Purchase order receipt history](docs/purchase%20order%20receipts.png)

### Purchase-order cancellation rules

An approved order can still be cancelled when no goods have been received. This provides a
controlled way to stop an order before it changes warehouse inventory.

![Cancelling an approved purchase order](docs/Cancel%20approved%20order.png)

Once an order has been fully received, cancellation is rejected. This protects the audit
trail and prevents the purchase-order status from becoming inconsistent with stock that has
already entered the warehouse.

![Received purchase order cannot be cancelled](docs/Can%20not%20cancel%20received%20purchase%20order.png)

Purchase orders can be listed and filtered by workflow status for operational monitoring.

![Listing purchase orders](docs/List%20purchase%20orders.png)

## Available warehouse stock

The stock API returns positive warehouse-product balances and supports filtering by warehouse
and product. Stock becomes available only after an approved purchase order is received.

![Listing available warehouse stock](docs/List%20available%20stock.png)

## Automated integration testing

Integration tests start an isolated PostgreSQL container and verify authentication,
authorization, validation, catalog data, purchase transitions, partial and full receipts,
stock accumulation, filtering and duplicate-reference handling.

![Successful local Testcontainers test run](docs/Local%20Test%20Run.png)

## Business rules demonstrated

- Public users cannot request elevated roles during registration.
- Only purchasing agents or administrators can create and submit purchase orders.
- Only inventory managers or administrators can approve and receive purchase orders.
- Draft and approved quantities do not appear in available stock before receipt.
- Receipt quantities cannot exceed the outstanding ordered quantity.
- Draft, submitted and unreceived approved orders may be cancelled.
- Partially or fully received orders cannot be cancelled.
- Duplicate products and duplicate purchase references are rejected.
- Stock receipt and purchase status changes are committed atomically.
- Database locking protects receipt processing from concurrent updates.

## Technology stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security and JWT |
| Persistence | Spring Data JPA and Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Documentation | OpenAPI and Swagger UI |
| Testing | JUnit, MockMvc and Testcontainers |
| Deployment | Docker and Docker Compose |

## Outcome

This project shows the implementation of a secure, transactional and testable REST backend
for a realistic multi-warehouse purchasing workflow. It is suitable as a foundation for
extensions such as product management, supplier administration, stock transfers, reorder
alerts, audit history and reporting.
