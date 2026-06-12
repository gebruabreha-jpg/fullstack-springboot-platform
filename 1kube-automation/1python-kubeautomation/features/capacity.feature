Feature: Capacity Testing

  Scenario: Scale deployment to test resource limits
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I scale deployment "eric-pc-sm-controller" to 10 replicas
    Then all pods should be running
    And resource usage should be below 80%
    And memory usage should be below 85%

  Scenario: Handle maximum concurrent connections
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When traffic load reaches 5000 concurrent connections
    Then response time should be under 200ms
    And error rate should be less than 1%

  Scenario: Pod eviction under resource pressure
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I consume node resources to trigger eviction
    Then pods are evicted gracefully
    And new pods are scheduled within 60 seconds