from sqlalchemy import String, Enum as SQLAEnum, DateTime, Text
from sqlalchemy.orm import Mapped, mapped_column
from datetime import datetime
from app.database import Base
import enum


class TaskState(str, enum.Enum):
    todo = "todo"
    in_progress = "in_progress"
    done = "done"


class Priority(str, enum.Enum):
    low = "low"
    medium = "medium"
    high = "high"


class Task(Base):
    __tablename__ = "tasks"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    state: Mapped[TaskState] = mapped_column(
        SQLAEnum(TaskState), default=TaskState.todo, nullable=False
    )
    priority: Mapped[Priority] = mapped_column(
        SQLAEnum(Priority), default=Priority.medium, nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, nullable=False
    )
    completed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
