from schema import BookCreate
from typing import Optional

class Book:
    def __init__(self, id: int, title: str, author: str, available: bool = True):
        self.id = id
        self.title = title
        self.author = author
        self.available = available