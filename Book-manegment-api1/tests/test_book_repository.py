from app.repositories.book import BookRepository
from app.schemas.book import BookCreate

def test_fetch_empty():
    repo = BookRepository()
    assert repo.fetch() == []

def test_add_and_fetch():
    repo = BookRepository()
    book = repo.add(BookCreate(title="Test", author="Author", available=True))
    assert book.id == 1
    assert book.title == "Test"
    assert len(repo.fetch()) == 1

def test_fetch_by_id_found():
    repo = BookRepository()
    repo.add(BookCreate(title="Test", author="Author", available=True))
    found = repo.fetch_by_id(1)
    assert found.title == "Test"

def test_fetch_by_id_not_found():
    repo = BookRepository()
    assert repo.fetch_by_id(999) is None

def test_delete():
    repo = BookRepository()
    repo.add(BookCreate(title="Test", author="Author", available=True))
    deleted = repo.delete(1)
    assert deleted.title == "Test"
    assert len(repo.fetch()) == 0

def test_delete_not_found():
    repo = BookRepository()
    assert repo.delete(999) is None