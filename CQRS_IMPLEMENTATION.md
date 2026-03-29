# CQRS Implementation Report (Hinglish)

Is document me detail me bataya gaya hai ki tumhare project me CQRS kahan implement hua hai, kaise flow karta hai, aur kaunse exact files involved hain.

## 1) Final Summary (Seedhi Baat)

Tumhare project me CQRS clearly **2 microservices** me implement hua hai:

1. `transaction-service`
2. `wallet-service`

Baaki services me full command/query split nahi mila.

---

## 2) CQRS Kya Implement Hua Hai Is Project Me

Yahan CQRS ka matlab:

- **Command side**: write/update operations (state change)
- **Query side**: read operations (read-only)
- **Event-driven cache invalidation**: RabbitMQ event + Redis/Spring cache invalidation

Yeh pattern dono target services me use hua hai.

---

## 3) `transaction-service` Me CQRS

## 3.1 Command Side (Write)

### Controller
- `transaction-service/src/main/java/com/wallet/transaction/command/controller/TransactionCommandController.java`
  - Endpoint: `POST /transactions`
  - Input: `CreateTransactionCommand`
  - Response: `202 Accepted` with `CommandStatusResponse`

### Command DTO
- `transaction-service/src/main/java/com/wallet/transaction/command/dto/CreateTransactionCommand.java`
  - Fields: `type`, `userId`, `senderId`, `receiverId`, `amount`, `originalTransactionId`, `idempotencyKey`

### Handler
- `transaction-service/src/main/java/com/wallet/transaction/command/handler/TransactionCommandHandler.java`
  - `handle(...)` command execute karta hai
  - Write ke baad `TransactionCreatedEvent` publish karta hai
  - `@CacheEvict(cacheNames = "transactionHistory", allEntries = true)` laga hua hai

### Domain Delegation
- `transaction-service/src/main/java/com/wallet/transaction/domain/TransactionDomainService.java`
  - command type ke hisab se `topup/transfer/payment/refund` ko `TransactionService` tak forward karta hai

### Event Publisher
- `transaction-service/src/main/java/com/wallet/transaction/event/TransactionEventPublisher.java`
  - RabbitMQ exchange `cqrs.events.exchange` par `transaction.events` key ke saath event bhejta hai

## 3.2 Query Side (Read)

### Controller
- `transaction-service/src/main/java/com/wallet/transaction/query/controller/TransactionQueryController.java`
  - Endpoint: `GET /transactions/{userId}`
  - Return: `List<TransactionReadDto>`

### Query Handler
- `transaction-service/src/main/java/com/wallet/transaction/query/handler/TransactionQueryHandler.java`
  - Read-only query method: `getTransactionsByUserId(...)`
  - `@Transactional(readOnly = true)`
  - `@Cacheable(cacheNames = "transactionHistory", key = "#userId")`

### Read DTO / Projection
- `transaction-service/src/main/java/com/wallet/transaction/query/dto/TransactionReadDto.java`
- `transaction-service/src/main/java/com/wallet/transaction/repository/TransactionReadProjection.java`
- `transaction-service/src/main/java/com/wallet/transaction/repository/TransactionRepository.java`
  - `findProjectedByUserIdOrSenderIdOrReceiverIdOrderByCreatedAtDesc(...)`

## 3.3 Event-based Cache Invalidation

- `transaction-service/src/main/java/com/wallet/transaction/query/handler/TransactionCacheInvalidationListener.java`
  - `@RabbitListener(queues = RabbitConfig.TRANSACTION_EVENTS_QUEUE)`
  - impacted users ke cache keys evict karta hai

---

## 4) `wallet-service` Me CQRS

## 4.1 Command Side (Write)

### Controller
- `wallet-service/src/main/java/com/wallet/wallet/command/controller/WalletCommandController.java`
  - Endpoint: `PUT /wallet/update`
  - Input: `WalletUpdateCommand`
  - Response: `202 Accepted` with `WalletCommandStatusResponse`

### Command DTO
- `wallet-service/src/main/java/com/wallet/wallet/command/dto/WalletUpdateCommand.java`
  - Fields: `type`, `userId`, `fromUserId`, `toUserId`, `amount`, limits, `commandId`

### Handler
- `wallet-service/src/main/java/com/wallet/wallet/command/handler/WalletCommandHandler.java`
  - `handle(...)` write command process karta hai
  - Idempotency ke liye Redis key use karta hai:
    - `wallet:command:{commandId}`
  - `@CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)`
  - Success par `WalletUpdatedEvent` publish karta hai

### Event Publisher
- `wallet-service/src/main/java/com/wallet/wallet/event/WalletEventPublisher.java`
  - `cqrs.events.exchange` par `wallet.events` key ke saath event publish karta hai

## 4.2 Query Side (Read)

### Controller
- `wallet-service/src/main/java/com/wallet/wallet/query/controller/WalletQueryController.java`
  - `GET /wallet/{userId}` -> balance
  - `GET /wallet/{userId}/transactions` -> wallet history

### Query Handler
- `wallet-service/src/main/java/com/wallet/wallet/query/handler/WalletQueryHandler.java`
  - `@Transactional(readOnly = true)`
  - `@Cacheable(cacheNames = "walletBalance", key = "#userId")`
  - `@Cacheable(cacheNames = "walletHistory", key = "#userId")`

## 4.3 Event-based Cache Invalidation

- `wallet-service/src/main/java/com/wallet/wallet/query/handler/WalletCacheInvalidationListener.java`
  - `@RabbitListener(queues = RabbitConfig.WALLET_CQRS_EVENTS_QUEUE)`
  - walletBalance + walletHistory caches evict karta hai impacted users ke liye

---

## 5) RabbitMQ CQRS Config Files

### Transaction service
- `transaction-service/src/main/java/com/wallet/transaction/config/RabbitConfig.java`
  - Exchange: `cqrs.events.exchange`
  - Queue: `transaction.events`
  - Routing key: `transaction.events`

### Wallet service
- `wallet-service/src/main/java/com/wallet/wallet/config/RabbitConfig.java`
  - Exchange: `cqrs.events.exchange`
  - Queue: `wallet.events`
  - Routing key: `wallet.events`

---

## 6) End-to-End Flow Diagram (Box Diagram)

```text
                         +-----------------------------------+
                         |           CLIENT / UI             |
                         +-----------------+-----------------+
                                           |
                    +----------------------+----------------------+
                    |                                             |
                    v                                             v
     +-------------------------------+            +--------------------------------+
     | Transaction Command API       |            | Wallet Command API             |
     | POST /transactions            |            | PUT /wallet/update             |
     | (TransactionCommandController)|            | (WalletCommandController)      |
     +---------------+---------------+            +---------------+----------------+
                     |                                            |
                     v                                            v
     +-------------------------------+            +--------------------------------+
     | TransactionCommandHandler     |            | WalletCommandHandler           |
     | - execute write               |            | - execute write                |
     | - publish TransactionCreated  |            | - Redis idempotency            |
     +---------------+---------------+            | - publish WalletUpdated        |
                     |                            +---------------+----------------+
                     |                                            |
                     +-------------------+------------------------+
                                         |
                                         v
                              +-------------------------+
                              | RabbitMQ CQRS Exchange  |
                              | cqrs.events.exchange    |
                              +-----------+-------------+
                                          / \
                                         /   \
                                        v     v
                     +-----------------------+   +-----------------------+
                     | transaction.events    |   | wallet.events         |
                     +-----------+-----------+   +-----------+-----------+
                                 |                           |
                                 v                           v
              +--------------------------------+   +--------------------------------+
              | TransactionCacheInvalidation   |   | WalletCacheInvalidation        |
              | evict transactionHistory cache |   | evict walletBalance/history    |
              +--------------------------------+   +--------------------------------+


     +-------------------------------+            +--------------------------------+
     | Transaction Query API         |            | Wallet Query API               |
     | GET /transactions/{userId}    |            | GET /wallet/{userId}           |
     | (TransactionQueryController)  |            | GET /wallet/{userId}/transactions|
     +---------------+---------------+            +---------------+----------------+
                     |                                            |
                     v                                            v
     +-------------------------------+            +--------------------------------+
     | TransactionQueryHandler       |            | WalletQueryHandler             |
     | @Cacheable(transactionHistory)|            | @Cacheable(walletBalance/history)
     +-------------------------------+            +--------------------------------+
```

---

## 7) Quick Validation Checklist (Tum Verify Kar Sakte Ho)

- [ ] `POST /transactions` command endpoint 202 de raha hai
- [ ] `GET /transactions/{userId}` query se data aa raha hai
- [ ] `PUT /wallet/update` command endpoint 202 de raha hai
- [ ] `GET /wallet/{userId}` aur `/wallet/{userId}/transactions` query data de rahe hain
- [ ] Command ke baad query result refresh ho raha hai (cache invalidation via RabbitMQ)

---

## 8) One-Line Interview Answer

"Project me CQRS `transaction-service` aur `wallet-service` me implement hai jahan command aur query controllers/handlers separate hain, writes ke baad RabbitMQ events publish hote hain, aur query-side Redis/Spring cache `@Cacheable` + listener-based invalidation se maintain hota hai."
