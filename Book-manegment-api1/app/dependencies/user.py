from fastapi import Depends
from app.repositories.user import UserRepository
from app.services.user import UserService

_repo = UserRepository()

def get_user_repo():
    return _repo

def get_user_service(repo = Depends(get_user_repo)):
    return UserService(repo)