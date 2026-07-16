import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.dependencies.book import get_book_repo, get_book_service
from app.dependencies.user import get_user_repo, get_user_service
from app.repositories.book import BookRepository
from app.repositories.user import UserRepository
from app.services.book import BookService
from app.services.user import UserService

# This fixture uses FastAPI's dependency injection to provide fresh
# repository and service instances for every test. This is a common
# testing strategy in modern FastAPI applications because it ensures
# test isolation and prevents shared state between tests.
@pytest.fixture
def client():
    book_repo = BookRepository()
    book_service = BookService(book_repo)
    user_repo = UserRepository()
    user_service = UserService(user_repo)

    def override_get_book_repo():
        return book_repo

    def override_get_book_service():
        return book_service

    def override_get_user_repo():
        return user_repo

    def override_get_user_service():
        return user_service

    app.dependency_overrides[get_book_repo] = override_get_book_repo
    app.dependency_overrides[get_book_service] = override_get_book_service
    app.dependency_overrides[get_user_repo] = override_get_user_repo
    app.dependency_overrides[get_user_service] = override_get_user_service

    yield TestClient(app)

    app.dependency_overrides.clear()
