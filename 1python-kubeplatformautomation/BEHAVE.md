In this project:-
    Cucumber = behave (Python BDD framework, equivalent to Cucumber)
    Translation = Behave parses features/1.feature and matches steps to steps/1.py

How it works:-
    Behave reads features/1.feature
    Each step (Given/When/Then) is matched by decorator in steps/1.py
    The @given("the full environment is set up") decorator on setup_all() function triggers it.The behave library is the Python equivalent of Cucumber/JVM Cucumber used in Beets.