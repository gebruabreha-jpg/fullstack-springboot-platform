from dataclasses import dataclass
from typing import Optional

@dataclass
class User:
    __tablename__ = "users"
    id: int
    name: str 
    email: str 