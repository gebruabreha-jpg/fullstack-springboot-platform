package io.github.chaosframework.api.exception;

public class HelmException extends ChaosException {
    public HelmException(String message) {
        super(message);
    }
    public HelmException(String message, Throwable cause) {
        super(message, cause);
    }
}
