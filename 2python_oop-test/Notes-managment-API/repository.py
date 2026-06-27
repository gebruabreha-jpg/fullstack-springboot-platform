from model import Note

class noterepository():
    def __init__(self):
        self.db = []
        self.id = 1
    
    def fetch(self):
        return self.db

    def fetch_by_id(self, id):
        for i in self.db:
            if i.id == id:
                return i
        return None
    
    def add (self, note1):
        my_note = Note(id=self.id, title=note1.title, content = note1.content, tag =note1.tag)
        self.db.append(my_note)
        self.id += 1
        return my_note

#Add type hints for clarity:
   #def fetch(self) -> list[Note]:
   #def fetch_by_id(self, id: int) -> Note | None:
   #def add(self, note1: NoteCreate) -> Note: