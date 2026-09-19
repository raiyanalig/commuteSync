# Testing CommuteSync

## Test pyramid

```text
        ┌─────────────────────────────┐
        │  Integration (*IT, Failsafe) │  full Spring context + H2 + MockMvc
        │  AuthAndAuthorizationIT      │  HTTP, security, DB, wiring
        │  TripLifecycleIT             │
        │  BookingSeatIT               │
        │  BookingRepositoryIT (@DataJpaTest)
        └─────────────────────────────┘
     ┌───────────────────────────────────────┐
     │  Unit (*Test, Surefire)               │  Mockito, no Spring, fast
     │  services, domain state machines,     │
     │  allocator, exception mapping         │
     └───────────────────────────────────────┘
```

## Structure

```text
src/test/java/com/commutesync/
├── auth/…                     unit: JWT, auth service, user details
├── employee/…                 unit: service + @WebMvcTest controller slice
├── fleet/…                    unit: driver/vehicle/assignment rules
├── route/… schedule/… trip/…  unit: services + state machines
├── booking/…                  unit: seat/capacity/cancellation rules
├── allocation/…               unit: pure allocator + service
├── passenger/… tracking/…     unit: manifest and tracking services
├── notification/… audit/…     unit: listeners and services
├── admin/…                    unit: dashboard aggregation
├── common/exception/          unit: GlobalExceptionHandlerTest
└── it/                        integration: *IT (Failsafe)
    ├── IntegrationTestSupport
    ├── AuthAndAuthorizationIT
    ├── TripLifecycleIT
    ├── BookingSeatIT
    └── BookingRepositoryIT
```

## Commands

```bash
mvn test        # unit tests only (Surefire, *Test)
mvn verify      # unit + integration tests (Surefire + Failsafe, *IT)
mvn -Dtest=BookingServiceTest test          # single unit test
mvn -Dit.test=BookingSeatIT verify          # single integration test
```

## What to mock vs. use the real database

**Mock (unit tests):**
- Repositories (`@Mock`) — verify the service's decisions and interactions.
- Collaborator services (e.g. `AuditService`, `NotificationService`, `ApplicationEventPublisher`).
- Nothing else: domain entities and state machines are real objects.

**Real database (integration tests):**
- Repository queries (`@DataJpaTest`) — availability/seat queries, constraints, derived queries.
- The full request path (`@SpringBootTest` + MockMvc) — security filters, JSON, transactions, exception mapping.
- Test profile uses H2 in PostgreSQL mode with Hibernate `create-drop` and Flyway disabled, so no Docker is required.

## Do / Don't

- Do test business behavior: state transitions, capacity limits, duplicates, authorization, exception mapping.
- Don't test getters/setters or framework behavior.
- Prefer asserting outcomes (status, persisted state) over implementation details.
