package io.github.chaosframework.api.exception;

public class InfraException extends ChaosException {
    public InfraException(String message) {
        super(message);
    }
    public InfraException(String message, Throwable cause) {
        super(message, cause);
    }
}
