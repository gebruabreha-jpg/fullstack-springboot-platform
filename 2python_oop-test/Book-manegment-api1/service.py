from model import Book
from schema import BookCreate
from repository import BookRepository

class BookService:
    def __init__(self, repo: BookRepository):
        self.repo = repo

    def create_book(self, book_data: BookCreate) -> Book:
        return self.repo.add(book_data)

    def get_all_books(self) -> list[Book]:
        return self.repo.fetch()

    def get_book_by_id(self, id: int) -> Book:
        book = self.repo.fetch_by_id(id)
        if not book:
            raise ValueError(f"Book with id {id} not found")
        return book

    def delete_book(self, id: int) -> Book:
        book = self.repo.delete(id)
        if not book:
            raise ValueError(f"Book with id {id} not found")
        return book