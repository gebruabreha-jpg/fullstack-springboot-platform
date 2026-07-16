🏦 Mini Banking Ledger System
It does 3 things:
Core capabilities:
💰 Store money (deposit)
💸 Take money out (withdraw)
🏦 Give loans + track repayment
📒 Record every action (ledger)

User Actions
   ↓
Account System (logic)
   ↓
Ledger (history / audit log)

System Architecture (Simple Version)
        ┌──────────────┐
        │   User API   │
        └──────┬───────┘
               ↓
        ┌──────────────┐
        │  Account     │  ← business logic
        └──────┬───────┘
               ↓
        ┌──────────────┐
        │  Ledger      │  ← history store
        └──────────────┘

You currently have 4 operations:-
Action	Meaning
deposit	add money
withdraw	remove money
take_loan	borrow money
repay_loan	pay back loan

Right now your design is:
❗ “Single Account class doing everything”
That is OK for learning.

But in real systems:
You would split:
Account Service (logic)
Ledger Service (history)
Loan Service (loan rules)


What YOU should focus on learning next
Before adding more code, understand:
Step 1:
✔ What is state?
balance
loan
Step 2:
✔ What is an event?
deposit
withdraw
loan
Step 3:
✔ What is a ledger?
history of events
Step 4:
✔ Why ledger matters?
debugging
audit
reconstruction

If system crashes, can I rebuild state?
If YES → ledger is correct design ✔


🔥 1. Transaction IDs (VERY important)
import uuid

txn_id = str(uuid.uuid4())
🔥 2. Double-entry ledger

Instead of:
LedgerEntry(txn_type, amount, balance_after)


For coding interviews, mention how you'd extend this design:-
Thread safety using locks.
Double-entry ledger (debit/credit records).
Transaction IDs and idempotency keys.
Persistence layer (PostgreSQL).
Interest calculation engine.
EMI schedule generation.
Event sourcing (ledger as source of truth).
Concurrency handling for simultaneous withdrawals.
Audit logging and reconciliation jobs.
