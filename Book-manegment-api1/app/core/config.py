from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "Book Management API"
    database_url: str = "sqlite:///./books.db"
    redis_url: str = "redis://localhost:6379"
    secret_key: str = "your-secret-key-here"

    class Config:
        env_file = ".env"

settings = Settings()