from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=False,
    )

    database_url: str = "postgresql+asyncpg://user:password@localhost:5432/task_db"
    secret_key: str = "your-secret-key-change-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    api_v1_str: str = "/api/v1"
    api_v2_str: str = "/api/v2"


settings = Settings()
