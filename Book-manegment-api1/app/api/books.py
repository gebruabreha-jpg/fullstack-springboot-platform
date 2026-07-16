from fastapi import APIRouter, Depends
from typing import List
from app.schemas.book import Book, BookCreate, BookUpdate
from app.services.book import BookService
from app.dependencies.book import get_book_service

router = APIRouter()

@router.post("/books", response_model=Book)
def create_book(
    book_data: BookCreate,
    service: BookService = Depends(get_book_service)
):
    return service.create_book(book_data)

@router.get("/books", response_model=List[Book])
def get_books(
    service: BookService = Depends(get_book_service)
):
    return service.get_all_books()

@router.get("/books/{book_id}", response_model=Book)
def get_book(
    book_id: int,
    service: BookService = Depends(get_book_service)
):
    return service.get_book_by_id(book_id)

@router.put("/books/{book_id}", response_model=Book)
def update_book(
    book_id: int,
    book_data: BookUpdate,
    service: BookService = Depends(get_book_service)
):
    return service.update_book(book_id, book_data)

@router.delete("/books/{book_id}", response_model=dict)
def delete_book(
    book_id: int,
    service: BookService = Depends(get_book_service)
):
    service.delete_book(book_id)
    return {"message": "Deleted"}