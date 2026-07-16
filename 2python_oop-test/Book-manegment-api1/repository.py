from model import Book
from schema import BookCreate
from typing import Optional

class BookRepository:
    def __init__(self):
        self.db: list[Book] = []
        self.id = 1
    
    def fetch(self) -> list[Book]:
        return self.db

    def fetch_by_id(self, id: int) -> Optional[Book]:
        for book in self.db:
            if book.id == id:
                return book
        return None
    
    def add(self, book_data: BookCreate) -> Book:
        book = Book(id=self.id, title=book_data.title, 
                    author=book_data.author, available=book_data.available)
        self.db.append(book)
        self.id += 1
        return book

    def delete(self, id: int) -> Optional[Book]:
        for index, book in enumerate(self.db):
            if book.id == id:
                return self.db.pop(index)
        return None
