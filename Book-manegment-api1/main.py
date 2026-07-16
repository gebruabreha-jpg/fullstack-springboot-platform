from fastapi import FastAPI
from app.api.books import router as books_router
from app.api.user import router as users_router

app = FastAPI()
app.include_router(books_router)
app.include_router(users_router)

@app.get("/")
def home():
    return {"status": "running"}