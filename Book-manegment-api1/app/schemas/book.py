from pydantic import BaseModel, ConfigDict, Field
from typing import Optional

#POST request body	
class BookCreate(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    title: str = Field(..., min_length=1)
    author: str = Field(..., min_length=1)
    available: bool = True
#Response body
class Book(BookCreate):
    id: int
#PUT request body
class BookUpdate(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    title: Optional[str] = Field(None, min_length=1)
    author: Optional[str] = Field(None, min_length=1)
    available: Optional[bool] = None