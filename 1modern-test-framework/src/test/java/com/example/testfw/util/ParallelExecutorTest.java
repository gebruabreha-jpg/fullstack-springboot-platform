package com.example.testfw.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ParallelExecutor utility using Java 21 StructuredTaskScope.
 */
class ParallelExecutorTest {

    @Test
    void testExecuteAll_success() throws Exception {
        // Arrange
        List<Callable<String>> tasks = List.of(
                () -> "Task 1",
                () -> "Task 2",
                () -> "Task 3"
        );

        // Act
        List<String> results = ParallelExecutor.executeAll(tasks);

        // Assert
        assertEquals(3, results.size());
        assertEquals("Task 1", results.get(0));
        assertEquals("Task 2", results.get(1));
        assertEquals("Task 3", results.get(2));
    }

    @Test
    void testExecuteAll_withFailure() throws Exception {
        // Arrange
        List<Callable<String>> tasks = List.of(
                () -> "Task 1",
                () -> { throw new RuntimeException("Task 2 failed"); },
                () -> "Task 3"
        );

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            ParallelExecutor.executeAll(tasks);
        });
        
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Task 2 failed", exception.getCause().getMessage());
    }

    @Test
    void testExecuteWithTimeout_success() throws Exception {
        // Arrange
        Callable<String> task = () -> {
            Thread.sleep(100); // Short delay
            return "Completed";
        };

        // Act
        String result = ParallelExecutor.executeWithTimeout(task, 1, TimeUnit.SECONDS);

        // Assert
        assertEquals("Completed", result);
    }

    @Test
    void testExecuteWithTimeout_timeout() throws Exception {
        // Arrange
        Callable<String> task = () -> {
            Thread.sleep(2000); // Long delay
            return "Completed";
        };

        // Act & Assert
        Exception exception = assertThrows(java.util.concurrent.TimeoutException.class, () -> {
            ParallelExecutor.executeWithTimeout(task, 100, TimeUnit.MILLISECONDS);
        });
        
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void testExecuteForEach() throws Exception {
        // Arrange
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        
        // Act
        List<String> results = ParallelExecutor.executeForEach(numbers, 
                num -> () -> "Number: " + num);

        // Assert
        assertEquals(5, results.size());
        assertEquals("Number: 1", results.get(0));
        assertEquals("Number: 5", results.get(4));
    }
}