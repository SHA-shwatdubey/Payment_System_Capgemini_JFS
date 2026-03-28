package com.wallet.transaction.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator class responsible for managing the execution of Saga steps.
 * It follows the LRA (Long Running Action) pattern by providing compensation
 * logic when a step fails.
 */
public class SagaOrchestrator<T> {
    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);
    private final List<SagaStep<T>> steps = new ArrayList<>();
    private final List<SagaStep<T>> completedSteps = new ArrayList<>();
    private final T context;

    public SagaOrchestrator(T context) {
        this.context = context;
    }

    public void addStep(SagaStep<T> step) {
        steps.add(step);
    }

    public void execute() {
        log.info("Starting Saga execution with context: {}", context);
        try {
            for (SagaStep<T> step : steps) {
                log.info("Executing Saga Step: {}", step.getName());
                step.execute(context);
                completedSteps.add(step); // Successful, add to checklist for rollback if needed
            }
            log.info("Saga execution completed successfully");
        } catch (Exception e) {
            log.error("Saga step failed. Starting compensation. Error: {}", e.getMessage());
            compensate();
            throw new RuntimeException("Saga execution failed. Compensation triggered: " + e.getMessage(), e);
        }
    }

    private void compensate() {
        // Reverse iterate to roll back in order: last successful step first
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = completedSteps.get(i);
            try {
                log.info("Compensating Saga Step: {}", step.getName());
                step.compensate(context);
            } catch (Exception e) {
                // Log and continue compensation for other steps if one fails.
                log.error("Compensation failed for step: {}. Error: {}", step.getName(), e.getMessage());
            }
        }
        log.info("Saga compensation completed");
    }
}
