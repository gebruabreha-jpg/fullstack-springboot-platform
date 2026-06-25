# Notes Management API
A simple Notes Management REST API built with **FastAPI** and **Object-Oriented Programming (OOP)**.
This project is part of a daily backend coding challenge series focused on improving API development and OOP design skills.
---
## Features
* Create notes
* Get all notes
* Search notes by keyword
* Delete notes
* In-memory storage (no database)

---
## Tech Stack
* Python 3.10+
* FastAPI
* Uvicorn

---

## Project Structure
```plaintext
notes-api/
│── main.py          # FastAPI app
│── models.py        # Note class
│── manager.py       # NoteManager class
│── requirements.txt
│── README.md
```
notes-api/ 
│── app/ │ 
│── main.py # Entry point │ 
│── api.py # FastAPI routes │ 
│── models.py # Note class / schemas │ 
│── logic.py # NoteManager business logic │ 
│── __init__.py │ 
│── requirements.txt │── README.md
---

## Installation
Clone the repository:
```bash
git clone <your-repo-url>
cd notes-api
```
Create virtual environment:
```bash
python -m venv venv
```
Activate virtual environment:

### Windows
```bash
venv\Scripts\activate
```
### Mac/Linux

```bash
source venv/bin/activate
```
Install dependencies:
```bash
pip install -r requirements.txt
```
---

## Run the Server

```bash
uvicorn main:app --reload

python -m uvicorn main:app --reload

```
Server will start at:

```plaintext
http://127.0.0.1:8000
```
Swagger docs:
```plaintext
http://127.0.0.1:8000/docs
```

---
## API Endpoints

### Create Note
**POST** `/notes`
Request body:
```json
{
  "title": "Learn FastAPI",
  "content": "Today I learned routing and dependency injection",
  "tags": ["python", "fastapi"]
}
```
Response:
```json
{
  "id": 1,
  "title": "Learn FastAPI",
  "content": "Today I learned routing and dependency injection",
  "tags": ["python", "fastapi"]
}
```

---
### Get All Notes
**GET** `/notes`
Returns all notes.
---
### Search Notes
**GET** `/notes/search?q=python`
Searches notes by:
* title
* content
* tags

---

### Delete Note
**DELETE** `/notes/{id}`
Deletes a note by ID.
Example:
```plaintext
DELETE /notes/1
```
---

## OOP Design
### Note Class
Represents a single note object.
Attributes:

* `id`
* `title`
* `content`
* `tags`

---

### NoteManager Class
Handles all business logic:
* create note
* fetch notes
* search notes
* delete notes

This keeps API routes clean and separates logic from HTTP handling.

---

## Future Improvements
* Add update note endpoint
* Add SQLite/PostgreSQL support
* Add authentication
* Add timestamps
* Add pagination
* Add tag filtering

---
## Learning Goals

This project helps practice:
* Object-Oriented Programming
* REST API development
* CRUD operations
* Search/filtering logic
* FastAPI fundamentals
---

## License
This project is for learning purposes.
