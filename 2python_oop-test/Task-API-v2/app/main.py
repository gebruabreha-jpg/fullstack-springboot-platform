from fastapi import FastAPI
from app.api.v1 import tasks as v1_tasks
from app.api.v2 import tasks as v2_tasks


app = FastAPI(
    title="Task API v2",
    description="Task management API with versioning",
    version="2.0.0",
)


app.include_router(v1_tasks.router, prefix="/api/v1/tasks", tags=["v1"])
app.include_router(v2_tasks.router, prefix="/api/v2/tasks", tags=["v2"])


@app.get("/")
def health():
    return {"status": "running", "version": "2.0.0"}
