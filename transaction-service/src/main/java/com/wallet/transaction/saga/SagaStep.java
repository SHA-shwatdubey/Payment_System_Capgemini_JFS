package com.wallet.transaction.saga;

/**
 * Interface representing a step in a Saga.
 * @param <T> The context type for the Saga
 */
public interface SagaStep<T> {
    /**
     * Logical name of the step.
     */
    String getName();

    /**
     * Executes the successful part of the step.
     */
    void execute(T context);

    /**
     * Executes the compensation/rollback logic for this step.
     * This is called if a subsequent step fails.
     */
    void compensate(T context);
}
