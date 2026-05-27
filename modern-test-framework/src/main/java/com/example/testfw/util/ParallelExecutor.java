package com.example.testfw.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modern parallel execution utility using Java 21 StructuredTaskScope and virtual threads.
 * Replaces legacy ParallelExecutionUtil with structured concurrency.
 */
public class ParallelExecutor {

    private ParallelExecutor() {
        // Utility class - prevent instantiation
    }

    /**
     * Executes multiple tasks in parallel using virtual threads.
     * 
     * @param tasks List of callable tasks to execute
     * @param <T>   Result type of tasks
     * @return List of results in same order as input tasks
     * @throws ExecutionException   If any task fails
     * @throws InterruptedException If interrupted while waiting
     */
    public static <T> List<T> executeAll(List<Callable<T>> tasks) 
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(tasks, "Tasks list cannot be null");
        
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Future<T>> futures = new ArrayList<>(tasks.size());
            
            // Fork all tasks in virtual threads
            for (Callable<T> task : tasks) {
                Future<T> future = scope.fork(task);
                futures.add(future);
            }
            
            // Wait for all tasks to complete or fail
            scope.joinUntil(Long.MAX_VALUE, TimeUnit.NANOS);
            
            // Propagate any exceptions
            scope.throwIfFailed();
            
            // Collect results in order
            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> future : futures) {
                results.add(future.resultNow());
            }
            return results;
        }
    }

    /**
     * Executes multiple tasks in parallel and returns results as they complete.
     * 
     * @param tasks List of callable tasks to execute
     * @param <T>   Result type of tasks
     * @return List of results in completion order
     * @throws ExecutionException   If any task fails
     * @throws InterruptedException If interrupted while waiting
     */
    public static <T> List<T> executeAnyOrder(List<Callable<T>> tasks) 
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(tasks, "Tasks list cannot be null");
        
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<Future<T>> futures = new ArrayList<>(tasks.size());
            
            // Fork all tasks in virtual threads
            for (Callable<T> task : tasks) {
                Future<T> future = scope.fork(task);
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            scope.joinUntil(Long.MAX_VALUE, TimeUnit.NANOS);
            
            // Propagate any exceptions
            scope.throwIfFailed();
            
            // Collect results
            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> future : futures) {
                results.add(future.resultNow());
            }
            return results;
        }
    }

    /**
     * Executes a single task with timeout using virtual threads.
     * 
     * @param task      Callable task to execute
     * @param timeout   Timeout duration
     * @param unit      Timeout time unit
     * @param <T>       Result type of task
     * @return Task result
     * @throws ExecutionException   If task fails
     * @throws InterruptedException If interrupted while waiting
     * @throws TimeoutException     If task times out
     */
    public static <T> T executeWithTimeout(Callable<T> task, long timeout, TimeUnit unit) 
            throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(unit, "Time unit cannot be null");
        
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Future<T> future = scope.fork(task);
            
            // Wait with timeout
            if (!scope.joinUntil(timeout, unit)) {
                future.cancel(true); // Cancel if timeout
                throw new java.util.concurrent.TimeoutException(
                        "Task timed out after " + timeout + " " + unit);
            }
            
            // Propagate any exceptions
            scope.throwIfFailed();
            
            return future.resultNow();
        }
    }

    /**
     * Executes tasks for each item in a collection in parallel.
     * 
     * @param items   List of items to process
     * @param worker  Function that processes each item
     * @param <T>     Input item type
     * @param <R>     Result type
     * @return List of results in same order as input items
     * @throws ExecutionException   If any task fails
     * @throws InterruptedException If interrupted while waiting
     */
    public static <T, R> List<R> executeForEach(List<T> items, 
                                                java.util.function.Function<T, Callable<R>> worker) 
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(items, "Items list cannot be null");
        Objects.requireNonNull(worker, "Worker function cannot be null");
        
        List<Callable<R>> tasks = new ArrayList<>(items.size());
        for (T item : items) {
            tasks.add(worker.apply(item));
        }
        
        return executeAll(tasks);
    }
}