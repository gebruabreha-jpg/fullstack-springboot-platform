from typing import List
from model import input
class TaskManager:
    def __init__(self):
        self.tasks = []
        #self.tasks: List[input] = []
        self.counter=1
    def add_task(self, title:str):
        task=input(self.counter, title)
        self.counter += 1
        self.tasks.append(task)
        return task

    def get_task(self, task_id: int) -> input | None:
        for task in self.tasks:
            if task.id == task_id:
                return task
        return None

    def delete_task(self, task_id: int) -> bool:
        for i, task in enumerate(self.tasks):
            if task.id == task_id:
                del self.tasks[i]
                return True
        return False

    def update_task(self, task_id: int, title: str | None = None, done: bool | None = None) -> Task | None:
        task = self.get_task(task_id)
        if task:
            if title is not None:
                task.title = title
            if done is not None:
                task.done = done
        return task

    def list_tasks(self) -> List[input]:
        return self.tasks