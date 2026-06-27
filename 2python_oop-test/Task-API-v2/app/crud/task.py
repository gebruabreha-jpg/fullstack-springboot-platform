from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, desc, text
from datetime import datetime
from app.models.task import Task, TaskState, Priority


class TaskCRUD:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create(self, title: str, description: str | None,
                     state: TaskState, priority: Priority,
                     owner_id: int | None = None) -> Task:
        task = Task(
            title=title,
            description=description,
            state=state,
            priority=priority,
        )
        self.db.add(task)
        await self.db.commit()
        await self.db.refresh(task)
        return task

    async def get_by_id(self, task_id: int) -> Task | None:
        result = await self.db.execute(
            select(Task).where(Task.id == task_id)
        )
        return result.scalar_one_or_none()

    async def get_all(
        self,
        skip: int = 0,
        limit: int = 10,
        state: str | None = None,
        priority: str | None = None,
    ) -> tuple[list[Task], int]:
        query = select(Task)
        count_query = select(func.count(Task.id))

        if state:
            query = query.where(Task.state == state)
            count_query = count_query.where(Task.state == state)
        if priority:
            query = query.where(Task.priority == priority)
            count_query = count_query.where(Task.priority == priority)

        total_result = await self.db.execute(count_query)
        total = total_result.scalar_one()

        query = query.offset(skip).limit(limit).order_by(desc(Task.created_at))
        result = await self.db.execute(query)
        return list(result.scalars().all()), total

    async def update(self, task_id: int, **kwargs) -> Task | None:
        task = await self.get_by_id(task_id)
        if not task:
            return None
        for key, value in kwargs.items():
            if value is not None and hasattr(task, key):
                setattr(task, key, value)
        if kwargs.get("state") == TaskState.done:
            task.completed_at = datetime.utcnow()
        await self.db.commit()
        await self.db.refresh(task)
        return task

    async def delete(self, task_id: int) -> bool:
        task = await self.get_by_id(task_id)
        if not task:
            return False
        await self.db.delete(task)
        await self.db.commit()
        return True

    async def get_completed_per_week(self) -> list[dict]:
        query = text("""
        SELECT
            id,
            title,
            state,
            completed_at,
            DATE_TRUNC('week', completed_at) AS week_start,
            COUNT(*) OVER (
                PARTITION BY DATE_TRUNC('week', completed_at)
            ) AS tasks_completed_that_week
        FROM tasks
        WHERE state = 'done'
          AND completed_at >= NOW() - INTERVAL '30 days'
        ORDER BY week_start DESC
        """)
        result = await self.db.execute(query)
        rows = result.all()
        return [
            {
                "id": row.id,
                "title": row.title,
                "state": row.state,
                "completed_at": row.completed_at.isoformat() if row.completed_at else None,
                "week_start": row.week_start.isoformat() if row.week_start else None,
                "tasks_completed_that_week": row.tasks_completed_that_week,
            }
            for row in rows
        ]
