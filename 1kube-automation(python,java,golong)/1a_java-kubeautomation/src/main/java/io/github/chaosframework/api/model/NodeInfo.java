package io.github.chaosframework.api.model;

public record NodeInfo(String name, boolean ready, boolean schedulable, String[] taints) {}
