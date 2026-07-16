from app.models.book import BookModel
from app.schemas.book import BookCreate, BookUpdate
from app.repositories.book import BookRepository
from app.exceptions.book import BookNotFoundError

class BookService:
    def __init__(self, repo: BookRepository):
        self.repo = repo

    def create_book(self, book_data: BookCreate) -> BookModel:
        return self.repo.add(book_data)

    def get_all_books(self) -> list[BookModel]:
        return self.repo.fetch()

    def get_book_by_id(self, id: int) -> BookModel:
        book = self.repo.fetch_by_id(id)
        if not book:
            raise BookNotFoundError(id)
        return book

    def update_book(self, id: int, book_data: BookUpdate) -> BookModel:
        book = self.repo.fetch_by_id(id)
        if not book:
            raise BookNotFoundError(id)
        updated = self.repo.update(id, book_data)
        if not updated:
            raise BookNotFoundError(id)
        return updated

    def delete_book(self, id: int) -> BookModel:
        book = self.repo.delete(id)
        if not book:
            raise BookNotFoundError(id)
        return book