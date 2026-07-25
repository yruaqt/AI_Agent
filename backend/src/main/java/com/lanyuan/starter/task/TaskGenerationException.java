package com.lanyuan.starter.task;

public class TaskGenerationException extends RuntimeException {
    public TaskGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskGenerationException(String message) {
        super(message);
    }
}
