Feature: Performance Testing

  Scenario: API response time under load
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I send 1000 HTTP requests to service endpoint "/health"
    Then 95th percentile response time should be under 100ms
    And throughput should be above 500 requests per second

  Scenario: Latency under stress conditions
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When CPU utilization reaches 80%
    Then latency should be under 50ms
    And throughput should be above 100 requests per second

  Scenario: Database query performance
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I execute database query "SELECT * FROM sessions WHERE active = true"
    Then query response time should be under 50ms