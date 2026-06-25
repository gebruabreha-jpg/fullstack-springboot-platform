Feature: Full E2E System Test

  Scenario: Run full infrastructure test suite
    Given the full environment is set up
    When all tests are executed
    Then the system should be cleaned up

  Scenario: Quick cluster connection test (requires cluster)
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I check pod health
    Then all pods should be running