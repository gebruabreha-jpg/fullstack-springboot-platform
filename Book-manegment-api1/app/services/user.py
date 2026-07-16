from app.repositories.user import UserRepository
from app.models.user import User
from app.schemas.user import CreateUser, UpdateUser
from app.exceptions.user import UserNotFoundError


#Its job is to coordinate business rules and use the repository to access data.
class UserService:

#Receives a UserRepository object and Stores it in self.repo so every method can use it.
    def __init__(self, repo: UserRepository):
        self.repo = repo

    def add_new_user(self, user_data: CreateUser) -> User:
        #before add Checks if the email already exists.
        existing = self.repo.fetch_by_email(user_data.email)
        if existing:
            raise ValueError(f"Email {user_data.email} already registered")
        return self.repo.add_user(user_data)

    def get_all_users(self) -> list[User]:
        return self.repo.fetch_all()

    def get_user_by_id(self, id: int) -> User:
        user = self.repo.fetch_by_id(id)
        if not user:
            raise UserNotFoundError(id)
        return user

    def update_user(self, id: int, user_data: UpdateUser) -> User:
        user = self.repo.fetch_by_id(id)
        if not user:
            raise UserNotFoundError(id)
        
        if user_data.email and user_data.email != user.email:
            existing = self.repo.fetch_by_email(user_data.email)
            if existing:
                raise ValueError(f"Email {user_data.email} already registered")
        
        updated = self.repo.update_user(id, user_data)
        if not updated:
            raise UserNotFoundError(id)
        return updated


    def delete_user(self, id: int) -> User:
        user = self.repo.delete_user(id)
        if not user:
            raise UserNotFoundError(id)
        return user