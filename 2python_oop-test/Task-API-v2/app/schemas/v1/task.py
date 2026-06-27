from pydantic import BaseModel


class TaskBaseV1(BaseModel):
    title: str
    description: str | None = None
    status: str


class TaskCreateV1(TaskBaseV1):
    pass


class TaskResponseV1(TaskBaseV1):
    id: int

    model_config = {"from_attributes": True}
