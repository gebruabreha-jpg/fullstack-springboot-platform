from fastapi import APIRouter, HTTPException
from typing import List
from schema import Book, BookCreate
from repository import BookRepository
from service import BookService

router = APIRouter()
repo = BookRepository()
service = BookService(repo)

@router.post("/books", response_model=Book)
def create_book(book_data: BookCreate):
    try:
        return service.create_book(book_data)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))

@router.get("/books", response_model=List[Book])
def get_books():
    return service.get_all_books()

@router.get("/books/{book_id}", response_model=Book)
def get_book(book_id: int):
    try:
        return service.get_book_by_id(book_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.delete("/books/{book_id}")
def delete_book(book_id: int):
    try:
        service.delete_book(book_id)
        return {"message": "Deleted"}
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))