# System Architecture Diagram (Mermaid)

```mermaid
graph TD
    subgraph ClientLayer [Client Layer]
        MobileApp[Mobile / Web Application]
    end

    subgraph InfrastructureLayer [Infrastructure Layer]
        Gateway[API Gateway (Spring Cloud)]
        Discovery[Eureka Service Discovery]
        Config[Config Server (Centralized Git)]
    end

    subgraph CoreServices [Core Application Layer]
        Auth[Auth Service (JWT, OTP)]
        Transaction[Transaction Service (Saga Orchestrator)]
        Wallet[Wallet Service (Ledger, Limits)]
        UserKYC[User-KYC Service (Documentation)]
        Rewards[Rewards Service (Points/Cashback)]
        Notification[Notification Service (Async Alerts)]
        Admin[Admin Service (Audit/Limits)]
    end

    subgraph SupportServices [Support & External Layer]
        Integration[Integration Service (Mock Providers)]
        Postgres[(PostgreSQL Databases)]
        Redis[(Redis Cache/Idempotency)]
        RabbitMQ([RabbitMQ Message Broker])
    end

    %% Communication Flows
    MobileApp --> Gateway
    Gateway --> Auth
    Gateway --> Transaction
    Gateway --> Wallet
    
    %% Discovery & Config
    CoreServices -.-> Discovery
    CoreServices -.-> Config

    %% Synchronous Calls
    Transaction -- Feign --> Wallet
    Transaction -- Feign --> Integration
    Wallet -- Feign --> Integration
    Auth -- Feign --> Notification

    %% Asynchronous Events
    Transaction -- Publish --> RabbitMQ
    Wallet -- Publish --> RabbitMQ
    RabbitMQ -- Consume --> Notification
    RabbitMQ -- Consume --> Rewards

    %% Data Integrity
    Wallet -- Caching/Idempotency --> Redis
    Transaction -- Caching --> Redis
    CoreServices -- Persistent Data --> Postgres

    %% Styles
    classDef infra fill:#f9f,stroke:#333,stroke-width:2px;
    classDef domain fill:#fff,stroke:#007bff,stroke-width:2px;
    classDef support fill:#e1f5fe,stroke:#01579b,stroke-width:1px;

    class Gateway,Discovery,Config infra;
    class Auth,Transaction,Wallet,UserKYC,Rewards,Notification,Admin domain;
    class Postgres,Redis,RabbitMQ support;
```

---

## **Diagram Logic Breakdown**
1.  **Entry Point**: All requests flow through the **API Gateway** after being redirected by the **Mobile/Web App**.
2.  **Discovery**: Every service registers its dynamic IP/Port with **Eureka** to enable high-availability load balancing.
3.  **Cross-Service Consistency**:
    - **Transaction** syncs with **Wallet** and **Integration** for real-time payment validation (Saga Orchestration).
    - **Notification** is primarily event-driven, ensuring it doesn't slow down the main client-facing APIs.
4.  **Resilience Layer**:
    - **Redis** is used as a caching mechanism (Query Side) and as a 24-hour TTL lock for idempotency (Command Side).
5.  **Event Bus**: **RabbitMQ** handles background processing for non-critical paths like cashback calculation and message delivery.
