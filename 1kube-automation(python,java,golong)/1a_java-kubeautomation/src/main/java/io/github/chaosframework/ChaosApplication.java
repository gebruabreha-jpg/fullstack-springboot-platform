package io.github.chaosframework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosApplication {
    private static final Logger log = LoggerFactory.getLogger(ChaosApplication.class);

    public static void main(String[] args) {
        log.info("Chaos Framework starting...");
        log.info("Kubernetes Chaos Automation - ready for experiments");
    }
}
