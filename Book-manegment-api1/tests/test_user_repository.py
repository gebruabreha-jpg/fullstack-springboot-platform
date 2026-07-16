from app.repositories.user import UserRepository
from app.schemas.user import CreateUser

def test_fetch_empty():
    repo = UserRepository()
    assert repo.fetch_all() == []

def test_add_and_fetch():
    repo = UserRepository()
    user = repo.add_user(CreateUser(name="Test", email="test@example.com"))
    assert user.id == 1
    assert user.name == "Test"
    assert len(repo.fetch_all()) == 1

def test_fetch_by_email():
    repo = UserRepository()
    repo.add_user(CreateUser(name="Test", email="test@example.com"))
    found = repo.fetch_by_email("test@example.com")
    assert found.name == "Test"