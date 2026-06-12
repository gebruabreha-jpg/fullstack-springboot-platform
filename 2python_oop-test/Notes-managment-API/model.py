##Contains your OOP model (Note) and request schemas.
##from pydantic import BaseModel whenever you need to enforce data structure and type validation at runtime
from pydantic import BaseModel 
from typing import List

class note:
    def __init__(self):
        self.id=id
        self.title=title
        self.content=content
        self.tag=tag
class notecreat(BaseModel):
    title:str
    content:str
    tag =list[str]
