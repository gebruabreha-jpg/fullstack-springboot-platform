package com.example.testfw.config;

import java.nio.file.Path;

public record TestConfig(String namespace, String cluster, Path kubeconfig) {}