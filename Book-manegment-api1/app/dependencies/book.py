from fastapi import Depends
from app.repositories.book import BookRepository
from app.services.book import BookService

_repo = BookRepository()

def get_book_repo():
    return _repo

def get_book_service(repo = Depends(get_book_repo)):
    return BookService(repo)