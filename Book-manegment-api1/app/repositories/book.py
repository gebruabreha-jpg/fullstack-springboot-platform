from app.models.book import BookModel
from app.schemas.book import BookCreate, BookUpdate
from typing import Optional

class BookRepository:
    def __init__(self):
        self.db: list[BookModel] = []
        self.id = 1
    
    def fetch(self) -> list[BookModel]:
        return self.db

    def fetch_by_id(self, id: int) -> Optional[BookModel]:
        for book in self.db:
            if book.id == id:
                return book
        return None
    
    def add(self, book_data: BookCreate) -> BookModel:
        book = BookModel(id=self.id, title=book_data.title, 
                         author=book_data.author, available=book_data.available)
        self.db.append(book)
        self.id += 1
        return book

    def update(self, id: int, book_data: BookUpdate) -> Optional[BookModel]:
        for index, book in enumerate(self.db):
            if book.id == id:
                updated = BookModel(
                    id=book.id,
                    title=book_data.title if book_data.title is not None else book.title,
                    author=book_data.author if book_data.author is not None else book.author,
                    available=book_data.available if book_data.available is not None else book.available
                )
                self.db[index] = updated
                return updated
        return None

    def delete(self, id: int) -> Optional[BookModel]:
        for index, book in enumerate(self.db):
            if book.id == id:
                return self.db.pop(index)
        return None