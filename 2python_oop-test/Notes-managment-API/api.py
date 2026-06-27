from fastapi import APIRouter, HTTPException
from typing import List
from model import Note, NoteCreate
from repository import NoteRepository
from service import NoteService

router = APIRouter()
repository1 = NoteRepository()
service1 = NoteService(repository1)


@router.post("/notes", response_model=Note)
def create_note(note_data: NoteCreate):
    try:
        return service1.create_note(note_data)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

@router.get("/notes", response_model=List[Note])
def get_notes():
    return repository1.fetch()

@router.get("/notes/search")
def search_notes(q: str = ""):
    return service1.search_notes(q)
    
@router.get("/notes/{note_id}", response_model=Note)
def get_note_by_id(note_id: int):
    note = repository1.fetch_by_id(note_id)
    if not note:
        raise HTTPException(status_code=404, detail="note not found")
    return note

@router.delete("/notes/{note_id}")
def delete_note(note_id: int):
    note = repository1.delete(note_id)
    if not note:
        raise HTTPException(status_code=404, detail="Note not found")
    return {"message": "Deleted"}