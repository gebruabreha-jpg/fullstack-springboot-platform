package io.github.chaosframework.api.model;

public record HelmRelease(String name, String namespace, String chart, String appVersion, String revision, String status, int testPassed, int testFailed) {}
