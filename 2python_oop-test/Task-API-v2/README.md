# Task API v2

Task management API demonstrating API versioning with FastAPI, SQLAlchemy async, and PostgreSQL.

## Features

- **API Versioning**: v1 and v2 endpoints running simultaneously
- **PostgreSQL + SQLAlchemy 2.0 async**: Real database with async operations
- **Alembic migrations**: Schema version control
- **Advanced SQL**: Window function analytics (tasks completed per week)
- **Pydantic v2**: Separate schemas for each API version

## Project Structure

```
Task-API-v2/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI app entry point
│   ├── config.py            # Pydantic Settings
│   ├── database.py          # Async engine + session
│   ├── models/
│   │   └── task.py          # SQLAlchemy ORM
│   ├── schemas/
│   │   ├── v1/task.py       # v1 Pydantic models
│   │   └── v2/task.py       # v2 Pydantic models
│   ├── crud/
│   │   └── task.py          # Database operations
│   ├── services/
│   │   └── task.py          # Business logic
│   └── api/
│       ├── v1/tasks.py      # v1 endpoints
│       └── v2/tasks.py      # v2 endpoints
├── alembic/                 # Database migrations
├── tests/
├── .env
└── requirements.txt
```

## Quick Start

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Start PostgreSQL
```bash
docker run --name task-db -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=task_db -p 5432:5432 -d postgres
```

### 3. Configure `.env`
Update `DATABASE_URL` with your credentials.

### 4. Run migrations
```bash
alembic init alembic
# Update alembic/env.py to import app.database.Base and settings
alembic revision --autogenerate -m "create tasks table"
alembic upgrade head
```

### 5. Start server
```bash
uvicorn app.main:app --reload
```

## API Endpoints

### v1 (legacy - `status` field, no pagination)
```
POST   /api/v1/tasks
GET    /api/v1/tasks
GET    /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}
```

### v2 (current - `state` field, pagination, filters, analytics)
```
POST   /api/v2/tasks
GET    /api/v2/tasks?page=1&limit=10&state=todo&priority=high
GET    /api/v2/tasks/{id}
PATCH  /api/v2/tasks/{id}
DELETE /api/v2/tasks/{id}
GET    /api/v2/analytics/completed-per-week
```

## Key Differences: v1 vs v2

| Feature | v1 | v2 |
|---------|----|----|
| Status field | `status` | `state` |
| Priority | Default only | Explicit enum |
| Pagination | No | Yes (`skip`, `limit`) |
| Filtering | No | Yes (`state`, `priority`) |
| Update | No | `PATCH` supported |
| Analytics | No | `completed-per-week` |
| Response | Flat | Nested `owner` object |



You change a model  →  alembic detects diff  →  creates migration file
                                                      ↓
                                              uses script.py.mako as template
                                                      ↓
                                              runs upgrade() via env.py
                                                      ↓
                                              applies to DB using alembic.ini URL