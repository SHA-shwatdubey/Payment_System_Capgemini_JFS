# Detailed System Architecture Design

## 1. Overview
The **Capgemini Payment and Reward System** is a robust microservices-based financial platform designed for high-availability, consistent distributed transactions, and real-time monitoring. The architecture follows a multi-tier structure with independent services communicating via synchronous Feign clients and asynchronous messaging (RabbitMQ).

---

## 2. Core Architecture Components

### 2.1 API Infrastructure
- **API Gateway**: Acts as the single entry point. It handles request routing, load balancing, and security filtering.
- **Service Discovery (Eureka)**: Maintains a dynamic registry of all active microservice instances.
- **Centralized Configuration**: Spring Cloud Config fetches service-specific properties from a git-backed configuration repository.

### 2.2 Domain Services
- **Auth Service**: Manages user lifecycle, JWT-based security tokens, and OTP verification flows.
- **Transaction Service**: The orchestrator for all financial flows. Implements the **Saga Pattern** for distributed payment reliability.
- **Wallet Service**: Manages the ledger, real-time balance updates, and transaction limits.
- **User-KYC Service**: Handles user onboarding and document verification states.
- **Rewards Service**: Calculates bonus points and cashbacks based on transaction events.
- **Notification Service**: Consumes RabbitMQ events to send SMS/Email alerts asynchronously.
- **Admin Service**: Portal for managing system-wide thresholds and compliance.
- **Integration Service**: A dedicated mock provider for simulating external payment gateways and KYC authorities.

---

## 3. Advanced Design Patterns

### 3.1 Distributed Transactions (Saga Pattern)
The `transaction-service` uses an **Orchestration-based Saga** to ensure Atomicity and Consistency across services during a payment:
1.  **Initiation**: Create a `PENDING` transaction state.
2.  **Validation**: Verify user status and reserve funds.
3.  **Persistence**: Execute ledger entries and update balances.
4.  **Finalization**: Commit the success state and trigger downstream events (Rewards/Notifications).
5.  **Compensation**: If any step fails (e.g., Insufficient Balance), the orchestrator triggers a rollback, marking the transaction as `FAILED` and reversing any interim states.

### 3.2 Resilience & Performance (Redis)
- **Caching Layer**: Frequent read queries (e.g., `walletBalance`, `transactionHistory`) are cached in Redis for <10ms response times.
- **Idempotency Store**: Every wallet command is tracked via a 24-hour Redis TTL to prevent duplicate processing of the same request.
- **CQRS Synchronization**: RabbitMQ listeners monitor command-side updates to automatically invalidate stale cache keys on the query side.

### 3.3 Event-Driven Architecture (RabbitMQ)
Services are decoupled using message queues for non-blocking operations:
- `transactionHistory` updates are pushed to the background for archival.
- `rewardEvent` triggers cashback calculations without delaying the main payment flow.
- `notificationEvent` ensures the user is alerted even if the primary transaction service is under high load.

---

## 4. Technology Stack Summary
| Component | Technology |
|---|---|
| Framework | Spring Boot 3.x, Spring Cloud |
| Database | PostgreSQL (One per service) |
| Caching | Redis (Standalone/Cluster) |
| Messaging | RabbitMQ |
| Security | JWT, Spring Security |
| Monitoring | Micrometer, Prometheus, Actuator |
| API Communication | OpenFeign, LoadBalancer |

---

## 5. Design Diagram
A detailed visual representation is provided in the sibling file: [ARCHITECTURE_DIAGRAM.md](file:///c:/Users/Shashwat/OneDrive/Desktop/JAVA_FULL_STACK_CAPGEMINI_PAYMENT_AND_REWARD_SYSTEM/BACKEND_Payment_System_Capgemini/ARCHITECTURE_DIAGRAM.md)
