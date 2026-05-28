from behave import given, when, then
# Replace these stubs with your actual test harness (Selenium, API client, etc.)

@given('I am on the login page')
def step_open_login_page(context):
    # Example: context.browser.get("https://example.com/login")
    context.page = "login"

@when('I enter username "{username}" and password "{password}"')
def step_enter_credentials(context, username, password):
    context.username = username
    context.password = password
    # Example: context.browser.fill_username(username); context.browser.fill_password(password)

@when('I click the "{button}" button')
def step_click_button(context, button):
    # Example: context.browser.click_button(button)
    context.clicked_button = button

@then('I should see the welcome message "{message}"')
def step_check_welcome(context, message):
    # Example: assert context.browser.get_text(".welcome") == message
    expected = f"Welcome, {context.username}!"
    assert expected == message, f"Expected '{expected}', got something else"