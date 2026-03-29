# Redis Usage Report (Hinglish) - Project Scan

Is document me detail me bataya gaya hai ki tumhare project me Redis **kahan-kahan** use ho raha hai, **kis purpose** ke liye use ho raha hai, aur **runtime flow** kya hai.

---

## 1) Final Verdict (Seedhi Baat)

Redis production runtime me mainly **2 services** me active use ho raha hai:

1. `wallet-service`
2. `transaction-service`

In dono services me Redis ka use 2 tarike se ho raha hai:

- **Caching layer** (Spring Cache + Redis backend)
- **Idempotency key store** (direct Redis access via `StringRedisTemplate`, wallet command side)

---

## 2) Redis Infrastructure Kahan Defined Hai

## 2.1 Docker Level

File: `docker-compose.yml`

- Redis container:
  - Service: `wallet-redis`
  - Image: `redis:7-alpine`
  - Port: `6379:6379`
- `wallet-service` aur `transaction-service` dono me env vars pass ho rahe hain:
  - `SPRING_DATA_REDIS_HOST: wallet-redis`
  - `SPRING_DATA_REDIS_PORT: 6379`

---

## 2.2 Application Config Level

### Transaction Service
- `transaction-service/src/main/resources/application.yml`
  - `spring.data.redis.host`
  - `spring.data.redis.port`
  - `spring.cache.type: redis`

### Wallet Service
- `wallet-service/src/main/resources/application.yml`
  - `spring.data.redis.host`
  - `spring.data.redis.port`
  - `spring.cache.type: redis`

### Config Server Repo Values
- `github-config/transaction-service.yml`
- `github-config/wallet-service.yml`

Dono me redis host/port + `spring.cache.type=redis` configured hai.

---

## 2.3 Dependency Level (Maven)

### Wallet
- `wallet-service/pom.xml`
  - `spring-boot-starter-data-redis`
  - `spring-boot-starter-cache`

### Transaction
- `transaction-service/pom.xml`
  - `spring-boot-starter-data-redis`
  - `spring-boot-starter-cache`

---

## 2.4 Cache Enablement

Caching explicitly enable ki gayi hai:

- `wallet-service/src/main/java/com/wallet/wallet/WalletServiceApplication.java` -> `@EnableCaching`
- `transaction-service/src/main/java/com/wallet/transaction/TransactionServiceApplication.java` -> `@EnableCaching`

---

## 3) Redis Config Classes (Common Cache Behavior)

### Wallet RedisConfig
- `wallet-service/src/main/java/com/wallet/wallet/config/RedisConfig.java`

### Transaction RedisConfig
- `transaction-service/src/main/java/com/wallet/transaction/config/RedisConfig.java`

Dono services me same pattern:

- `RedisCacheManager` use ho raha hai
- serializer: `GenericJackson2JsonRedisSerializer`
- TTL: `Duration.ofMinutes(5)`
- null caching disabled (`disableCachingNullValues()`)

Matlab cache entries 5 min tak valid rehti hain jab tak manually evict na ho.

---

## 4) `wallet-service` Me Redis Ka Detailed Use

## 4.1 Query Caching (Read Side)

File: `wallet-service/src/main/java/com/wallet/wallet/query/handler/WalletQueryHandler.java`

- `@Cacheable(cacheNames = "walletBalance", key = "#userId")`
  - wallet balance read ko cache karta hai
- `@Cacheable(cacheNames = "walletHistory", key = "#userId")`
  - wallet history read ko cache karta hai

Use case: same user ka dashboard/data baar-baar fast serve ho.

---

## 4.2 Command Side Cache Eviction

File: `wallet-service/src/main/java/com/wallet/wallet/command/handler/WalletCommandHandler.java`

- `@CacheEvict(cacheNames = {"walletBalance", "walletHistory"}, allEntries = true)`

Jab write operation hota hai (topup/transfer/limits update), purana cached read data invalidate hota hai.

---

## 4.3 Direct Redis for Idempotency

File: `wallet-service/src/main/java/com/wallet/wallet/command/handler/WalletCommandHandler.java`

- `StringRedisTemplate` direct use hota hai
- key pattern: `wallet:command:{commandId}`
- `setIfAbsent(..., Duration.ofHours(24))`

Meaning:
- same commandId dubara aaya to duplicate command skip ho jata hai
- 24 ghante tak idempotency protection

Yeh Redis ka **direct operational use** hai (sirf cache annotation nahi).

---

## 4.4 Event-based Invalidation (CQRS Sync)

File: `wallet-service/src/main/java/com/wallet/wallet/query/handler/WalletCacheInvalidationListener.java`

- `@RabbitListener(queues = RabbitConfig.WALLET_CQRS_EVENTS_QUEUE)`
- impacted user IDs ke liye:
  - `walletBalance` cache evict
  - `walletHistory` cache evict

Yeh ensure karta hai ki command side update ke baad query side stale data na de.

---

## 5) `transaction-service` Me Redis Ka Detailed Use

## 5.1 Query Caching (Read Side)

File: `transaction-service/src/main/java/com/wallet/transaction/query/handler/TransactionQueryHandler.java`

- `@Cacheable(cacheNames = "transactionHistory", key = "#userId")`

Read model ko fast banane ke liye user transaction list cache hoti hai.

---

## 5.2 Service Layer Caching + Eviction

File: `transaction-service/src/main/java/com/wallet/transaction/service/TransactionService.java`

### Cache Eviction on writes
- `topup(...)` -> `@CacheEvict(transactionHistory, allEntries=true)`
- `transfer(...)` -> same
- `payment(...)` -> same
- `refund(...)` -> same

### Cacheable reads
- `getByUser(...)` -> `@Cacheable(transactionHistory, key=#userId)`
- `getHistory(from,to)` -> `@Cacheable(transactionHistory, key=from-to)`

---

## 5.3 Command Handler Eviction

File: `transaction-service/src/main/java/com/wallet/transaction/command/handler/TransactionCommandHandler.java`

- `@CacheEvict(cacheNames = "transactionHistory", allEntries = true)`

Command API se write hone par cache clear hota hai.

---

## 5.4 Event-based Invalidation (RabbitMQ)

### Listener 1
- `transaction-service/src/main/java/com/wallet/transaction/query/handler/TransactionCacheInvalidationListener.java`
- impacted user IDs ka targeted cache evict karta hai (`transactionHistory`)

### Listener 2
- `transaction-service/src/main/java/com/wallet/transaction/service/TransactionHistoryEventListener.java`
- event consume karte time `@CacheEvict(transactionHistory, allEntries=true)`

Note: transaction side me do invalidation paths exist karte hain (targeted + allEntries). Functional hai, but behavior broad ho sakta hai due to allEntries eviction.

---

## 6) Test Environment Note

Tests me Redis-based cache mostly disable hota hai:

- `transaction-service/src/test/resources/application.properties` -> `spring.cache.type=none`
- `wallet-service/src/test/resources/application.properties` -> `spring.cache.type=none`

Isliye unit tests cache layer ke bina run karte hain (deterministic testing ke liye useful).

---

## 7) Redis Data Types Used

Practical level par tumhare code me yeh Redis usage types hain:

1. **String key-value (direct)**
   - wallet idempotency command key
2. **Spring Cache entries (serialized JSON objects)**
   - walletBalance
   - walletHistory
   - transactionHistory

---

## 8) End-to-End Redis Flow (Box Diagram)

```text
                         +-----------------------+
                         |      Frontend/UI      |
                         +-----------+-----------+
                                     |
                  Read APIs          |         Write APIs
             (GET wallet/tx)         |      (topup/transfer/payment...)
                                     |
                    +----------------+----------------+
                    |                                 |
                    v                                 v
      +-----------------------------+   +-----------------------------+
      | Query Handlers              |   | Command/Service Handlers    |
      | WalletQueryHandler          |   | WalletCommandHandler         |
      | TransactionQueryHandler     |   | TransactionCommandHandler    |
      +--------------+--------------+   | TransactionService(write)    |
                     |                  +--------------+--------------+
         @Cacheable  |                                 |  @CacheEvict
                     v                                 v
              +-----------------------------------------------+
              |                   Redis                       |
              |  - walletBalance cache                        |
              |  - walletHistory cache                        |
              |  - transactionHistory cache                   |
              |  - wallet:command:{commandId} (idempotency)  |
              +-------------------+---------------------------+
                                  ^
                                  |
                    Cache miss -> DB read -> cache fill


                +---------------------------------------------+
                | RabbitMQ CQRS Events                        |
                +-------------------+-------------------------+
                                    |
                                    v
      +-------------------------------------------------------------+
      | Cache Invalidation Listeners                                |
      | WalletCacheInvalidationListener                             |
      | TransactionCacheInvalidationListener / TransactionHistory...|
      +-------------------+-----------------------------------------+
                          |
                          v
                    Redis cache keys evicted
```

---

## 9) Service-wise Quick Matrix

| Service | Redis Config | Cache Names | Direct Redis Ops | Event Invalidation |
|---|---|---|---|---|
| wallet-service | Yes | `walletBalance`, `walletHistory` | Yes (`wallet:command:{commandId}`) | Yes |
| transaction-service | Yes | `transactionHistory` | No direct StringRedisTemplate usage found | Yes |
| other services | No active runtime Redis usage found in current scan | - | - | - |

---

## 10) Interview-ready Answer (Short)

"Project me Redis `wallet-service` aur `transaction-service` me use hua hai. Query side pe Spring Cache (`@Cacheable`) se response fast hota hai, command/write pe `@CacheEvict` aur RabbitMQ listeners se cache invalidation hoti hai. Wallet command side me Redis ka direct idempotency use bhi hai (`wallet:command:{commandId}`, 24h TTL)."
