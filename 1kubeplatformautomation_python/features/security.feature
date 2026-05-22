Feature: Security Testing

  Scenario: Vulnerability scanning
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I run vulnerability scan on container images
    Then no critical vulnerabilities found
    And high severity vulnerabilities are documented

  Scenario: RBAC authorization test
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I attempt to access restricted resource as unauthorized user
    Then access is denied
    And audit log records the attempt

  Scenario: Network policy enforcement
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When I attempt connection between unauthorized pods
    Then connection is blocked
    And network policy violation is logged