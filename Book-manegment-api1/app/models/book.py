from dataclasses import dataclass

@dataclass
class BookModel:
    __tablename__ = "books"
    id: int
    title: str
    author: str
    available: bool = True