package com.lanyuan.starter.task;

import com.lanyuan.starter.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskStateMachineTest {

    private final TaskStateMachine stateMachine = new TaskStateMachine();

    @Test
    void acceptsConfiguredForwardTransitions() {
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.DRAFT, TaskStatus.CONFIRMED));
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.CONFIRMED, TaskStatus.TODO));
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.TODO, TaskStatus.DOING));
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.DOING, TaskStatus.DONE));
        assertDoesNotThrow(() -> stateMachine.validate(TaskStatus.DOING, TaskStatus.CANCELLED));
    }

    @Test
    void rejectsSkippingAndLeavingTerminalStatus() {
        assertThrows(BusinessException.class,
                () -> stateMachine.validate(TaskStatus.DRAFT, TaskStatus.DOING));
        assertThrows(BusinessException.class,
                () -> stateMachine.validate(TaskStatus.DONE, TaskStatus.TODO));
        assertThrows(BusinessException.class,
                () -> stateMachine.validate(TaskStatus.CANCELLED, TaskStatus.DRAFT));
    }
}
