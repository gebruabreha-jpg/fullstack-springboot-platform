Feature: Stability Testing

  Scenario: Long running test for 24 hours
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When system runs for 24 hours
    Then no critical errors should occur
    And availability should be 99.9%
    And no unexpected restarts

  Scenario: Memory leak detection
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I run workload for 4 hours
    Then memory usage should remain stable
    And no memory leak detected

  Scenario: Pod stability during rolling update
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I perform rolling update of deployment "eric-pc-sm-controller"
    Then all pods should be running during update
    And no traffic loss during update