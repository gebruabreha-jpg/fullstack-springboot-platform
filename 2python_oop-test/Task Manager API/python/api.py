from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from logic import TaskManager

app = FastAPI()
tm = TaskManager()

class TaskCreate(BaseModel):
    title: str

class TaskUpdate(BaseModel):
    title: str | None = None    
    done: bool | None = None

@app.get("/tasks")
def get_tasks():
    return [{"id": t.id, "title": t.title, "done": t.done} for t in tm.list_tasks()]

@app.post("/tasks", status_code=201)
def create_task(task: TaskCreate):
    t = tm.add_task(task.title)
    return {"id": t.id, "title": t.title, "done": t.done}

@app.get("/tasks/{task_id}")
def get_task(task_id: int):
    task = tm.get_task(task_id)
    if task:
        return {"id": task.id, "title": task.title, "done": task.done}
    raise HTTPException(status_code=404, detail="Task not found")

@app.put("/tasks/{task_id}")
def update_task(task_id: int, task: TaskUpdate):
    updated = tm.update_task(task_id, task.title, task.done)
    if updated:
        return {"id": updated.id, "title": updated.title, "done": updated.done}
    raise HTTPException(status_code=404, detail="Task not found")

@app.delete("/tasks/{task_id}", status_code=204)
def delete_task(task_id: int):
    if tm.delete_task(task_id):
        return
    raise HTTPException(status_code=404, detail="Task not found")