package io.github.chaosframework.api.exception;

public class ChaosException extends RuntimeException {
    public ChaosException(String message) {
        super(message);
    }
    public ChaosException(String message, Throwable cause) {
        super(message, cause);
    }
}
