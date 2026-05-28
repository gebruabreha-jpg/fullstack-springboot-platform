Feature: User login
  As a registered user
  I want to log in to the system
  So that I can access my dashboard

  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter username "alice" and password "secret123"
    And I click the "Login" button
    Then I should see the welcome message "Welcome, alice!"