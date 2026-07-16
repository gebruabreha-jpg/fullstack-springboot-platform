import pytest
from app.services.user import UserService
from app.repositories.user import UserRepository
from app.schemas.user import CreateUser, UpdateUser
from app.exceptions.user import UserNotFoundError

@pytest.fixture
def service():
    repo = UserRepository()
    return UserService(repo)

def test_add_new_user(service):
    user = service.add_new_user(CreateUser(name="Test", email="test@example.com"))
    assert user.id == 1
    assert user.name == "Test"

def test_add_new_user_duplicate_email(service):
    service.add_new_user(CreateUser(name="Test", email="test@example.com"))
    with pytest.raises(ValueError, match="already registered"):
        service.add_new_user(CreateUser(name="Test2", email="test@example.com"))

def test_get_all_users(service):
    service.add_new_user(CreateUser(name="User1", email="u1@example.com"))
    service.add_new_user(CreateUser(name="User2", email="u2@example.com"))
    users = service.get_all_users()
    assert len(users) == 2

def test_get_user_by_id(service):
    service.add_new_user(CreateUser(name="Test", email="test@example.com"))
    user = service.get_user_by_id(1)
    assert user.name == "Test"

def test_get_user_by_id_not_found(service):
    with pytest.raises(UserNotFoundError):
        service.get_user_by_id(999)