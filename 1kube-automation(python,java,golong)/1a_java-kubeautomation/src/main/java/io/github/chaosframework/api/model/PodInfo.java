package io.github.chaosframework.api.model;

public record PodInfo(String name, String namespace, String phase, String node, boolean ready, String[] containers) {}
