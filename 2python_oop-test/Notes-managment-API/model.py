##Contains your OOP model (Note) and request schemas.
##from pydantic import BaseModel whenever you need to enforce data structure and type validation at runtime
from pydantic import BaseModel 
from typing import List

class Note(BaseModel):
    id: int
    title: str
    content: str
    tag: list[str]

class NoteCreate(BaseModel):
    title: str
    content: str
    tag: list[str]
