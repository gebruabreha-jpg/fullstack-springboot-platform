from dataclasses import dataclass
from datetime import datetime


@dataclass
class LedgerEntry:
    txn_type: str
    amount: float
    balance_after: float
    timestamp: datetime


class Account:

    def __init__(self, account_id: str):
        self.account_id = account_id
        self.balance = 0.0
        self.loan_outstanding = 0.0
        self.ledger = []

    def _add_entry(self, txn_type, amount):
        self.ledger.append(
            LedgerEntry(
                txn_type,
                amount,
                self.balance,
                datetime.now()
            )
        )

    def deposit(self, amount):
        if amount <= 0:
            raise ValueError("Invalid deposit")

        self.balance += amount
        self._add_entry("DEPOSIT", amount)

    def withdraw(self, amount):
        if amount > self.balance:
            raise ValueError("Insufficient balance")

        self.balance -= amount
        self._add_entry("WITHDRAW", amount)

    def take_loan(self, amount):
        self.balance += amount
        self.loan_outstanding += amount
        self._add_entry("LOAN", amount)

    def repay_loan(self, amount):
        pay = min(amount, self.loan_outstanding)
        self.balance -= pay
        self.loan_outstanding -= pay
        self._add_entry("LOAN_REPAY", pay)


# -------------------------
# TESTING (same file)
# -------------------------

def test_account():
    acc = Account("A1")

    acc.deposit(1000)
    assert acc.balance == 1000

    acc.withdraw(300)
    assert acc.balance == 700

    acc.take_loan(5000)
    assert acc.balance == 5700
    assert acc.loan_outstanding == 5000

    acc.repay_loan(1000)
    assert acc.loan_outstanding == 4000

    print("All tests passed")


if __name__ == "__main__":
    test_account()

    acc = Account("DEMO")

    acc.deposit(2000)
    acc.take_loan(10000)
    acc.repay_loan(1500)

    print("\nBalance:", acc.balance)
    print("Loan:", acc.loan_outstanding)

    print("\nLedger:")
    for l in acc.ledger:
        print(l)