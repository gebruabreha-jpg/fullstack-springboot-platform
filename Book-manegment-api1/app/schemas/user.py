from typing import Optional
from pydantic import BaseModel, ConfigDict, EmailStr, Field

#That's because schemas are for data validation, not for CRUD operations.
#A common point of confusion is thinking every API endpoint needs its own schema. 
#In reality, schemas represent the shape of the data being sent or returne
#schema  mifelgu api and  respons schema always at the end
#get and elete user do not need schema
class CreateUser(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    name: str = Field(..., min_length=1)
    email: EmailStr


class UpdateUser(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    name: Optional[str] = Field(None, min_length=1)
    email: Optional[EmailStr] = None


class ReadUser(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    email: EmailStr