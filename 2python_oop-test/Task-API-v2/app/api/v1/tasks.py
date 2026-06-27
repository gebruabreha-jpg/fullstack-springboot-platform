from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db
from app.crud.task import TaskCRUD
from app.schemas.v1.task import TaskCreateV1, TaskResponseV1


router = APIRouter()


@router.post("/", response_model=TaskResponseV1)
async def create_task_v1(
    task_data: TaskCreateV1,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    task = await crud.create(
        title=task_data.title,
        description=task_data.description,
        state=task_data.status,
        priority="medium",
    )
    return task


@router.get("/", response_model=list[TaskResponseV1])
async def get_tasks_v1(
    skip: int = 0,
    limit: int = 10,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    tasks, _ = await crud.get_all(skip=skip, limit=limit)
    return tasks


@router.get("/{task_id}", response_model=TaskResponseV1)
async def get_task_v1(
    task_id: int,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    task = await crud.get_by_id(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task


@router.delete("/{task_id}")
async def delete_task_v1(
    task_id: int,
    db: AsyncSession = Depends(get_db),
):
    crud = TaskCRUD(db)
    deleted = await crud.delete(task_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Task not found")
    return {"message": "Deleted"}
