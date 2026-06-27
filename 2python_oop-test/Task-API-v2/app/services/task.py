from datetime import datetime
from app.models.task import Task, TaskState, Priority
from app.crud.task import TaskCRUD
from app.schemas.v2.task import TaskCreateV2, TaskUpdateV2


class TaskService:
    def __init__(self, crud: TaskCRUD):
        self.crud = crud

    async def create_task(self, task_data: TaskCreateV2) -> Task:
        return await self.crud.create(
            title=task_data.title,
            description=task_data.description,
            state=task_data.state,
            priority=task_data.priority,
        )

    async def update_task(self, task_id: int, task_data: TaskUpdateV2) -> Task | None:
        update_dict = task_data.model_dump(exclude_unset=True)
        if "state" in update_dict and update_dict["state"] == TaskState.done:
            update_dict["completed_at"] = datetime.utcnow()
        return await self.crud.update(task_id, **update_dict)

    async def get_completed_per_week(self) -> list[dict]:
        return await self.crud.get_completed_per_week()
