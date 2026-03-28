package com.wallet.transaction.saga;

import com.wallet.transaction.entity.Transaction;
import java.math.BigDecimal;

/**
 * Saga Context to store shared data between Saga steps.
 */
public class PaymentSagaContext {
    private Transaction transaction;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private Long ledgerDebitId;
    private Long ledgerCreditId;

    public PaymentSagaContext(Long senderId, Long receiverId, BigDecimal amount) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Long getLedgerDebitId() { return ledgerDebitId; }
    public void setLedgerDebitId(Long ledgerDebitId) { this.ledgerDebitId = ledgerDebitId; }
    public Long getLedgerCreditId() { return ledgerCreditId; }
    public void setLedgerCreditId(Long ledgerCreditId) { this.ledgerCreditId = ledgerCreditId; }

    @Override
    public String toString() {
        return "PaymentSagaContext{" +
                "senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", amount=" + amount +
                '}';
    }
}
