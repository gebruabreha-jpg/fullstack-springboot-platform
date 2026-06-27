from model import Note, NoteCreate
from repository import NoteRepository


class NoteService:
    def __init__(self, repo: NoteRepository):
        self.repo = repo
        self.MAX_TITLE_LENGTH = 100
        self.FORBIDDEN_WORDS = ["spam", "test"]

    def create_note(self, note_data: NoteCreate) -> Note:
        self._validate(note_data)
        return self.repo.add(note_data)

    def get_notes_by_tag(self, tag: str) -> list[Note]:
        return [n for n in self.repo.fetch() if tag in n.tags]

    def search_notes(self, query: str) -> list[Note]:
        query = query.lower()
        return [
            n for n in self.repo.fetch()
            if query in n.title.lower() or query in n.content.lower()
        ]

    def _validate(self, note_data: NoteCreate):
        if len(note_data.title) > self.MAX_TITLE_LENGTH:
            raise ValueError(f"Title too long (max {self.MAX_TITLE_LENGTH})")
        for word in self.FORBIDDEN_WORDS:
            if word in note_data.title.lower():
                raise ValueError(f"Forbidden word: {word}")
        if not note_data.tags:
            raise ValueError("At least one tag is required")