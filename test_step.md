# Creating Step Definitions — Key Lessons
## 1. Fix the Gherkin FIRST
Don't blindly implement bad Gherkin. Actions use `When`, not `Then`.
```gherkin
# ❌ Wrong
Then delete and terminate active session on the node function PGW
# ✅ Correct
When the active session on the node function PGW is deleted and terminated
```

## 2. Reuse Before Creating
Before writing a new step, check if it already exists in:
- `beets`
- `tacos`
- `pcc-framework`
Place your step in the right class (e.g., GWC-related → `GwcNodeStepDefinition`) to reuse injected objects.

## 3. Prefer Enums Over Strings
Use custom Cucumber parameter types for finite value sets instead of raw `{string}`.
```java
// ❌ Weak — no validation, runtime errors
@Then("delete and terminate active session on the node function {string}")
public void deleteAndTerminateNodeFunction(final String nodeFunction)

// ✅ Strong — type-safe, compile-time checks
@Then("delete and terminate active session on the node function {node_function}")
public void deleteAndTerminateNodeFunction(final NodeFunctions nodeFunction)
```

Register the custom parameter type in `CucumberTypes`:
```java
parameterTypes.add(new ParameterType<>("node_function", nodeFunctionStr, NodeFunctions.class,
    (final String s) -> NodeFunctions.valueOf(s.toUpperCase())));
```

## 4. Use Dependency Injection
Don't create objects manually — inject them via Guice:
```java
@Inject
private GwcNode gwcNode;
```
Then delegate the real work:
```java
gwcNode.deleteAndTerminateNodeFunction(0, nodeFunction);
```

Step definitions should be a **thin layer** — they orchestrate, not implement.
## 5. Write Javadoc
Every step definition must have:
- Description of purpose
- Example Gherkin
- Parameter docs

```java
/**
 * Deletes and terminates active session on a node function.
 *
 * <b>Example</b>
 * <pre class="brush:feature">
 * When the active session on the node function PGW is deleted and terminated
 * </pre>
 * @param nodeFunction
 *        The node function to delete and terminate (e.g. PGW, SGW).
 */
```

## 6. Use @CriticalTestStep for Critical Logic
When a failure should stop the entire test execution:
```java
@CriticalTestStep
@When("the active session on the node function {node_function} is deleted and terminated")
public void deleteAndTerminateNodeFunction(final NodeFunctions nodeFunction) {
    gwcNode.deleteAndTerminateNodeFunction(0, nodeFunction);
}
```

## 7. Step Definition Template
```java
public class GwcNodeStepDefinition implements TestStepDefinition {
    @Inject
    private GwcNode gwcNode;

    /**
     * Deletes and terminates active session on a node function.
     *
     * <b>Example</b>
     * <pre class="brush:feature">
     * When the active session on the node function PGW is deleted and terminated
     * </pre>
     * @param nodeFunction
     *        The node function to target.
     */
    @CriticalTestStep
    @When("the active session on the node function {node_function} is deleted and terminated")
    public void deleteAndTerminateNodeFunction(final NodeFunctions nodeFunction) {
        gwcNode.deleteAndTerminateNodeFunction(0, nodeFunction);
    }
}
```

## Quick Reference

| Rule | Why |
|------|-----|
| Fix Gherkin before coding | Correct design prevents rework      |
| Reuse existing steps      | Avoid duplication and inconsistency |
| Thin step definitions     | Delegate logic to service objects   |
| Enums over strings        | Type safety, no runtime surprises   |
| Dependency injection      | Loose coupling, shared state        |
| Javadoc on every step     | Code should teach the next developer|
