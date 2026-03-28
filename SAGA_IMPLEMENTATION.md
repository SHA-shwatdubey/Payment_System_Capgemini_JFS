# Saga Design Pattern Implementation in NexPay

The **Saga Pattern** has been implemented to manage distributed transactions across microservices, ensuring data consistency without using heavy distributed locks (2PC).

## 1. Architectural Approach: Orchestration
We chose **Orchestration-based Saga** because it provides a centralized controller (the `SagaOrchestrator`) that manages the flow and compensation logic, making the transaction state easy to track.

## 2. Core Components
- **`SagaStep<T>`**: An interface that defines the contract for each step in a Saga.
  - `execute(T context)`: Logic to perform the forward operation.
  - `compensate(T context)`: Logic to undo the operation if a later step fails.
- **`SagaOrchestrator<T>`**: The engine that executes steps in order and triggers a "Reverse Execution" (Compensation) if any step throws an exception.
- **`PaymentSagaContext`**: A shared object that stores state (like Transaction ID, Ledger IDs) between steps.

## 3. The Payment Saga Flow
In `TransactionService.payment()`, the following steps are executed:

| Step | Name | Action | Compensation (Rollback) |
| :--- | :--- | :--- | :--- |
| **1** | `CreateTransaction` | Create a `PENDING` transaction record in DB. | Mark transaction as `FAILED`. |
| **2** | `ValidateAndReserve` | Check user KYC status and wallet balance. | N/A (Read-only). |
| **3** | `AccountingEntries` | Create Debit/Credit ledger entries and mark `SUCCESS`. | Log reversal (would usually perform a refund). |
| **4** | `PublishRewardEvent` | Send event to `rewards-service` to award points. | Log reward cancellation. |

## 4. Why this is a Saga?
- **Atomicity**: If the Rewards step (External service) fails, the Orchestrator will automatically trigger the compensation of previous steps (Marking transaction as FAILED).
- **Resilience**: It handles partial failures by ensuring we don't leave the system in an inconsistent state (e.g., money deducted but no transaction record).
- **Visibility**: The `PaymentSagaContext` provides a clear log of where the transaction failed.

---
**How to explain this in an interview/presentation:**
> "In my project, I implemented the Saga pattern for the Payment flow using an Orchestration approach. I created a generic `SagaOrchestrator` that manages a sequence of `SagaStep` objects. Each step has both an `execute` and a `compensate` method. If any part of the payment (like awarding rewards via another service) fails, the orchestrator automatically rolls back preceding steps (like marking the transaction as failed), ensuring eventual consistency across my microservices."
