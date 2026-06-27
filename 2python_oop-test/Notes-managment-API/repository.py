from model import Note, NoteCreate
from typing import Optional

class NoteRepository:
    def __init__(self):
        self.db: list[Note] = []
        self.id = 1
    
    def fetch(self) -> list[Note]:
        return self.db

    def fetch_by_id(self, id: int) -> Optional[Note]:
        for note in self.db:
            if note.id == id:
                return note
        return None
    
    def add(self, note_data: NoteCreate) -> Note:
        my_note = Note(id=self.id, title=note_data.title, content=note_data.content, tags=note_data.tags)
        self.db.append(my_note)
        self.id += 1
        return my_note

    def delete(self, id: int) -> Optional[Note]:
        for index, note in enumerate(self.db):
            if note.id == id:
                return self.db.pop(index)
        return None