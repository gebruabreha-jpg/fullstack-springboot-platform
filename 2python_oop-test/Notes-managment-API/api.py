from fastapi import APIRouter, HTTPException
from model import Note, NoteCreate
from repository import noterepository
from service import noteservice

router = APIRouter()
repository1 = noterepository()
service1 = noteservice(repository1)


@router.post("/notes", response_model=Note)
def create_note(note_data: NoteCreate):
    #return repository1.add(note_data)
    return service1.create_note(note_data)

@router.get("/notes", response_model=List[Note])
def get_notes():
    return repository1.fetch()
    
@router.get("/notes/{note_id}", response_model = Note)
def get_note_by_id(note_id: int):
    note = repository1.fetch_by_id(note_id)
    if not note:
        raise HTTPException(status_code =404, detail ="note not found")
    retun note

@router.delete("/notes/{note_id}")
def delete_note(note_id: int):
    note = repository1.delete(note_id)
    if not note:
        raise HTTPException(status_code=404, detail="Note not found")
    return {"message": "Deleted"}