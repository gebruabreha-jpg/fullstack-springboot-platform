Feature: Container Kill Robustness Test

  Scenario: Kill container controlled by resource
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When a container that controlled by resource is killed
      | Pod Resource          | Container  | Kill Signal | Number of pods | Role   |
      | eric-pc-sm-controller | controller | sigkill     | 1              | active |
    Then container restart count values have increased
    And total signaling success rate is above 99.999%


Feature: Pod Delete Robustness Test

  Scenario: Delete pods controlled by resource
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When pods controlled by resource are deleted
      | Resource                   | Role    | Max random resource |
      | eric-pc-sm-controller      | active  | 1                   |
    Then pods recover within 60 seconds
    And no alarms have been active


Feature: Worker Node Failure Robustness Test

  Scenario: Worker node kernel panic
    Given I load environment "test"
    And I set namespace to "kwok-test"
    When a worker node with a minority of KVDB master pods is kernel panicked
    Then system recover within 120 seconds
    And packet loss is less than 5 packets per million loss