from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db
from app.crud.task import TaskCRUD
from app.services.task import TaskService
from app.schemas.v2.task import (
    TaskCreateV2,
    TaskUpdateV2,
    TaskResponseV2,
)


router = APIRouter()


@router.post("/", response_model=TaskResponseV2)
async def create_task_v2(
    task_data: TaskCreateV2,
    db: AsyncSession = Depends(get_db),
):
    service = TaskService(TaskCRUD(db))
    task = await service.create_task(task_data)
    return task


@router.get("/", response_model=list[TaskResponseV2])
async def get_tasks_v2(
    skip: int = 0,
    limit: int = 10,
    state: str | None = None,
    priority: str | None = None,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    tasks, total = await crud.get_all(
        skip=skip, limit=limit, state=state, priority=priority
    )
    return tasks


@router.get("/{task_id}", response_model=TaskResponseV2)
async def get_task_v2(
    task_id: int,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    task = await crud.get_by_id(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@router.patch("/{task_id}", response_model=TaskResponseV2)
async def update_task_v2(
    task_id: int,
    task_data: TaskUpdateV2,
    db: AsyncSession = Depends(get_db),
):
    service = TaskService(TaskCRUD(db))
    task = await service.update_task(task_id, task_data)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@router.delete("/{task_id}")
async def delete_task_v2(
    task_id: int,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    deleted = await crud.delete(task_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"message": "Deleted"}


@router.get("/analytics/completed-per-week")
async def completed_per_week(db: AsyncSession = Depends(get_db)):
    crud = TaskCRUD(db)
    data = await crud.get_completed_per_week()
    return data
