from fastapi import APIRouter, HTTPException, Depends 
from typing import List
from app.dependencies.user import get_user_service
from app.schemas.user import ReadUser, CreateUser, UpdateUser
from app.services.user import UserService 

router = APIRouter()

@router.post("/users", response_model=ReadUser)
def create_user(user_data: CreateUser, service: UserService = Depends(get_user_service)):
    return service.add_new_user(user_data)

@router.get("/users", response_model=List[ReadUser])
def get_users(
    service: UserService = Depends(get_user_service)
):
    return service.get_all_users()

@router.get("/users/{user_id}", response_model=ReadUser)
def get_user(
    user_id: int,
    service: UserService = Depends(get_user_service)
):
    return service.get_user_by_id(user_id)

@router.put("/users/{user_id}", response_model=ReadUser)
def update_user(
    user_id: int,
    user_data: UpdateUser,
    service: UserService = Depends(get_user_service)
):
    return service.update_user(user_id, user_data)

@router.delete("/users/{user_id}", response_model=dict)
def delete_user(user_id: int, service: UserService = Depends(get_user_service)):
    service.delete_user(user_id)
    return {"message": "Deleted"}