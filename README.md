# Inventory Management API

First milestone of a multi-warehouse inventory management backend built with Java 21 and Spring Boot.

This milestone provides secure user registration, JWT login, role administration, Swagger UI, PostgreSQL migrations, Docker deployment, and PostgreSQL Testcontainers integration tests.

## Roles

- `ADMIN` — lists users and assigns roles
- `INVENTORY_MANAGER` — reserved for inventory operations in the next milestone
- `PURCHASING_AGENT` — reserved for supplier and purchase-order operations
- `VIEWER` — default role assigned during public registration

Public registration deliberately cannot request an elevated role. An administrator must assign elevated permissions.

## API routes

| Method | Path | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register with the `VIEWER` role |
| `POST` | `/api/auth/login` | Public | Obtain a JWT |
| `GET` | `/api/users/me` | Authenticated | View the current profile |
| `GET` | `/api/users` | `ADMIN` | List users |
| `PATCH` | `/api/users/{id}/role` | `ADMIN` | Assign a role |
| `GET` | `/actuator/health` | Public | Health check |

## Run with Docker

Prerequisite: Docker Desktop with the Linux container engine running.

For local demonstration:

```powershell
docker compose up --build
```

Docker Compose creates:

- PostgreSQL on the internal Docker network
- API at http://localhost:8080
- Swagger UI at http://localhost:8080/swagger-ui.html

Demo administrator:

```text
Email: admin@example.com
Password: admin12345
```

These credentials are development defaults only. Override them before any non-local deployment:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String(
    [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
$env:BOOTSTRAP_ADMIN_EMAIL = "your-admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "replace-with-a-strong-password"
docker compose up --build
```

## Test with Swagger

1. Open Swagger UI.
2. Call `POST /api/auth/login` with the administrator credentials.
3. Copy the returned `token`.
4. Select **Authorize** and enter the token. Swagger adds the `Bearer` prefix.
5. Call `GET /api/users`.
6. Register a normal user.
7. Use `PATCH /api/users/{id}/role` to assign `INVENTORY_MANAGER`.

## Run integration tests

Prerequisites: JDK 21, Maven 3.9+, and Docker Desktop.

```powershell
mvn test
```

The tests start an isolated `postgres:17-alpine` container and verify:

- Registration and login
- JWT-protected profile access
- Administrator user listing
- Role assignment
- Viewer authorization rejection
- Duplicate registration
- Request validation and Problem Details responses

Tests are skipped gracefully when Docker is unavailable.

## Configuration

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret, minimum 32 bytes |
| `JWT_EXPIRATION_MS` | Token lifetime |
| `BOOTSTRAP_ADMIN_EMAIL` | Initial administrator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | Initial administrator password |
| `BOOTSTRAP_ADMIN_DISPLAY_NAME` | Initial administrator name |

Flyway owns the database schema. Hibernate uses `ddl-auto=validate`.

## Next milestone

Add warehouses, products, categories, and warehouse-level inventory balances with role-protected REST endpoints.
