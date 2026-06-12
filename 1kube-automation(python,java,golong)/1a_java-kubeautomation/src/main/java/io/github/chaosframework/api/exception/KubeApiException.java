package io.github.chaosframework.api.exception;

public class KubeApiException extends ChaosException {
    public KubeApiException(String message) {
        super(message);
    }
    public KubeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
