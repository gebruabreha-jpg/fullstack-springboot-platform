from app.models.user import User
from app.schemas.user import CreateUser, UpdateUser
from typing import Optional

#Initialize a simple in-memory database (a Python list) to store User objects.
#The constructor (__init__) runs automatically and creates:-
#self.db = [] → an empty list that acts as the database.
#self.id = 1 → the starting ID for new users.
class UserRepository:
    def __init__(self):
        self.db: list[User] = []
        self.id = 1
    
    def fetch_all(self) -> list[User]:
        return self.db 
    
    def fetch_by_id(self, id: int) -> Optional[User]:
        for user in self.db:
            if user.id == id:
                return user
        return None

    def fetch_by_email(self, email: str) -> Optional[User]:
        for user in self.db:
            if user.email == email:
                return user
        return None
    
    def add_user(self, user_data: CreateUser) -> User:
        user = User(id=self.id, name=user_data.name, email=user_data.email)
        self.db.append(user)
        self.id += 1
        return user

    def update_user(self, id: int, user_data: UpdateUser) -> Optional[User]:
        for index, user in enumerate(self.db):
            if user.id == id:
                updated = User(
                    id=user.id,
                    name=user_data.name if user_data.name is not None else user.name,
                    email=user_data.email if user_data.email is not None else user.email
                )
                self.db[index] = updated
                return updated
        return None

    def delete_user(self, id: int) -> Optional[User]:
        for index, user in enumerate(self.db):
            if user.id == id:
                return self.db.pop(index)
        return None