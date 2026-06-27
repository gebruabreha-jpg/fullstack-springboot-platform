from pydantic import BaseModel
from typing import Optional

class BookCreate(BaseModel):
    title :str
    author : str
    available :bool = True

class Book(BookCreate):
    id: int

