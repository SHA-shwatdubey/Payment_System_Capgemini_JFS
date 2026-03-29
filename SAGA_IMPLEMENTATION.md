# Saga Pattern Kahan Aur Kaise Use Hua Hai (Hinglish Guide)

Yeh file simple Hinglish me explain karti hai ki tumhare project me Saga pattern exactly kahan use hua hai aur runtime me kaise kaam karta hai.

## 1) Final Result (Seedhi Baat)

Saga pattern tumhare project me **mainly 1 core flow** me use hua hai:

- **Service:** `transaction-service`
- **Flow:** `payment(...)`
- **Style:** Orchestration-based Saga (custom orchestrator class ke through)

Baaki services me full Saga framework implement nahi hai.

---

## 2) Saga Ki Main Files

### A) Saga ka Core Engine

1. `transaction-service/src/main/java/com/wallet/transaction/saga/SagaStep.java`
   - Har step ka contract define karta hai:
     - `getName()`
     - `execute(context)`
     - `compensate(context)`

2. `transaction-service/src/main/java/com/wallet/transaction/saga/SagaOrchestrator.java`
   - Steps ko ek-ek karke run karta hai.
   - Jo steps success hote hain unko track karta hai.
   - Agar beech me failure aaye to reverse order me compensation run karta hai.

3. `transaction-service/src/main/java/com/wallet/transaction/saga/PaymentSagaContext.java`
   - Shared data object hai jo har step use karta hai.
   - Isme senderId, receiverId, amount, transaction details etc. store hote hain.

### B) Jahan Saga Actually Start Hota Hai

4. `transaction-service/src/main/java/com/wallet/transaction/service/TransactionService.java`
   - Method: `payment(PaymentRequest request)`
   - Yahi pe context banta hai, orchestrator banta hai, steps add hote hain, aur `execute()` call hota hai.

---

## 3) Request Saga Tak Kaise Pahuchti Hai

### Direct REST API path

- `transaction-service/src/main/java/com/wallet/transaction/controller/TransactionController.java`
  - Endpoint: `POST /transactions/payment`
  - Yeh call karta hai `transactionService.payment(request)`

### CQRS Command path

- `transaction-service/src/main/java/com/wallet/transaction/command/handler/TransactionCommandHandler.java`
  - Agar command type `PAYMENT` hai to `transactionDomainService.payment(...)` call hota hai

- `transaction-service/src/main/java/com/wallet/transaction/domain/TransactionDomainService.java`
  - Aage `transactionService.payment(...)` delegate karta hai

Matlab REST se aao ya command se, final Saga wali payment method hi run hoti hai.

---

## 4) `payment(...)` Me Saga Ke Steps (Simple Flow)

### Step 1: `CreateTransaction`
- Transaction ko `PENDING` state me create karta hai
- Context me transaction set karta hai
- **Compensation:** agar baad me fail hua to transaction ko `FAILED` mark karta hai

### Step 2: `ValidateAndReserve`
- Sender aur receiver validate karta hai
- Balance check karta hai
- **Compensation:** nahi (read/validation type step hai)

### Step 3: `AccountingEntries`
- Ledger debit/credit entries banata hai
- Transaction ko `SUCCESS` mark karta hai
- **Compensation:** currently log karta hai ki reversal hona chahiye (future me proper reverse command add ho sakta hai)

### Step 4: `PublishRewardEvent`
- Reward event publish karta hai
- Transaction history event publish karta hai
- **Compensation:** rollback intent log karta hai (placeholder behavior)

Agar kisi bhi step pe error aata hai to orchestrator reverse compensation chala deta hai.

---

## 5) Cross-Service Integration (RabbitMQ)

Payment Saga ka Step 4 doosri service ko event bhejta hai:

- Publisher: `transaction-service/src/main/java/com/wallet/transaction/service/RewardEventPublisher.java`
- Rabbit config: `transaction-service/src/main/java/com/wallet/transaction/config/RabbitConfig.java`
  - Exchange: `wallet.events.exchange`
  - Queue: `wallet.events.queue`
- Consumer: `rewards-service/src/main/java/com/wallet/rewards/service/WalletEventListener.java`
  - `@RabbitListener(queues = "wallet.events.queue")`

Yeh distributed side-effect hai Saga flow ka.

---

## 6) Box Diagram (Aasan Samajh)

```text
+-----------------------+          +-----------------------------------+
| TransactionController |          | TransactionCommandHandler (CQRS)  |
| POST /transactions/   |          | type=PAYMENT                      |
| payment               |          +------------------+----------------+
+-----------+-----------+                             |
            |                                         |
            +--------------------+--------------------+
                                 |
                                 v
                 +--------------------------------------+
                 | TransactionService.payment(...)      |
                 |  SagaOrchestrator<PaymentSagaContext>|
                 +------------------+-------------------+
                                    |
      +-----------------------------+-----------------------------+
      |                             |                             |
      v                             v                             v
+-------------+             +---------------+             +----------------+
| Step 1      |             | Step 2        |             | Step 3         |
| CreateTxn   |             | Validate      |             | Accounting     |
| PENDING     |             | users+balance |             | ledger +SUCCESS|
+------+------+             +-------+-------+             +--------+-------+
       |                            |                               |
       +----------------------------+-------------------------------+
                                    |
                                    v
                           +---------------------+
                           | Step 4 PublishEvent |
                           | wallet.events.queue |
                           +----------+----------+
                                      |
                                      v
                          +-------------------------+
                          | rewards-service listener |
                          | WalletEventListener      |
                          +-------------------------+

Failure hua -> reverse order me compensation run hota hai.
```

---

## 7) Important Notes

- Saga abhi primary tareeke se `payment` flow me use hua hai.
- `topup`, `transfer`, `refund` methods transactional hain, but same SagaOrchestrator flow use nahi karte.
- Ledger/reward compensation ka kuch part placeholder logging mode me hai; production me isko aur strict rollback commands ke saath improve kiya ja sakta hai.

---

## 8) Interview Me Kaise Bolna Hai

"Hamare project me Saga pattern `transaction-service` ke payment flow me use hota hai. Custom `SagaOrchestrator` 4 steps run karta hai: transaction create, validation, accounting, aur reward event publishing. Agar koi step fail ho jaye to compensation reverse order me run hoti hai, jisse distributed consistency maintain hoti hai. Reward side effect RabbitMQ ke through `rewards-service` tak jata hai."
