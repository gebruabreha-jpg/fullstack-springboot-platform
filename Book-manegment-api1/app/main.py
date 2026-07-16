from fastapi import FastAPI, HTTPException
from app.api.books import router as books_router
from app.api.user import router as users_router
from app.exceptions.book import BookNotFoundError
from app.exceptions.user import UserNotFoundError

app = FastAPI()
app.include_router(books_router)
app.include_router(users_router)

@app.exception_handler(BookNotFoundError)
async def book_not_found_handler(request, exc):
    raise HTTPException(status_code=404, detail=str(exc))

@app.exception_handler(UserNotFoundError)
async def user_not_found_handler(request, exc):
    raise HTTPException(status_code=404, detail=str(exc))

@app.get("/")
def home():
    return {"status": "running"}