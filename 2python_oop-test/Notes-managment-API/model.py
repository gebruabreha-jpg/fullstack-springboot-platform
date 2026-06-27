from pydantic import BaseModel
from typing import List

class Note(BaseModel):
    id: int
    title: str
    content: str
    tags: list[str]

class NoteCreate(BaseModel):
    title: str
    content: str
    tags: list[str]
