import pytest
from app.services.book import BookService
from app.repositories.book import BookRepository
from app.schemas.book import BookCreate, BookUpdate
from app.exceptions.book import BookNotFoundError

@pytest.fixture
def service():
    repo = BookRepository()
    return BookService(repo)

def test_create_book(service):
    book = service.create_book(BookCreate(title="Test", author="Author", available=True))
    assert book.id == 1
    assert book.title == "Test"

def test_get_all_books(service):
    service.create_book(BookCreate(title="Book 1", author="Author 1", available=True))
    service.create_book(BookCreate(title="Book 2", author="Author 2", available=False))
    books = service.get_all_books()
    assert len(books) == 2
    assert books[0].title == "Book 1"
    assert books[1].title == "Book 2"

def test_get_book_by_id(service):
    service.create_book(BookCreate(title="Test", author="Author", available=True))
    book = service.get_book_by_id(1)
    assert book.title == "Test"

def test_get_book_by_id_not_found(service):
    with pytest.raises(BookNotFoundError):
        service.get_book_by_id(999)

def test_update_book(service):
    service.create_book(BookCreate(title="Old Title", author="Author", available=True))
    updated = service.update_book(1, BookUpdate(title="New Title"))
    assert updated.title == "New Title"
    assert updated.author == "Author"
    assert updated.available == True

def test_delete_book(service):
    service.create_book(BookCreate(title="Test", author="Author", available=True))
    book = service.delete_book(1)
    assert book.title == "Test"

def test_delete_book_not_found(service):
    with pytest.raises(BookNotFoundError):
        service.delete_book(999)