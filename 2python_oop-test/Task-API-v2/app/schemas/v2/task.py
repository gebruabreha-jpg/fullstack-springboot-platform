from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum


class TaskStateV2(str, Enum):
    todo = "todo"
    in_progress = "in_progress"
    done = "done"


class PriorityV2(str, Enum):
    low = "low"
    medium = "medium"
    high = "high"


class OwnerV2(BaseModel):
    id: int
    name: str


class TaskBaseV2(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    description: str | None = Field(None, max_length=5000)
    state: TaskStateV2
    priority: PriorityV2


class TaskCreateV2(TaskBaseV2):
    pass


class TaskUpdateV2(BaseModel):
    title: str | None = Field(None, min_length=1, max_length=200)
    description: str | None = Field(None, max_length=5000)
    state: TaskStateV2 | None = None
    priority: PriorityV2 | None = None


class TaskResponseV2(BaseModel):
    id: int
    title: str
    description: str | None
    state: TaskStateV2
    priority: PriorityV2
    created_at: datetime
    completed_at: datetime | None
    owner: OwnerV2 | None = None

    model_config = {"from_attributes": True}
