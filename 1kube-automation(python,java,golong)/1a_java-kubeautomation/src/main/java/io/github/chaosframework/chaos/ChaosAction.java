package io.github.chaosframework.chaos;

public interface ChaosAction {
    String getName();
    String getDescription();
    ChaosResult execute(ChaosContext context);
    void cleanup(ChaosContext context);
    default int getTimeoutSeconds() { return 300; }
    default boolean isCleanupSupported() { return true; }
}
