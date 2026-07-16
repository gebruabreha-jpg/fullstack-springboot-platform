import pytest
from fastapi.testclient import TestClient
from app.main import app

def test_create_book(client):
    response = client.post("/books", json={
        "title": "Learn FastAPI",
        "author": "Wizard",
        "available": True
    })
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == 1
    assert data["title"] == "Learn FastAPI"

def test_get_books(client):
    client.post("/books", json={"title": "Book 1", "author": "A1", "available": True})
    client.post("/books", json={"title": "Book 2", "author": "A2", "available": False})
    response = client.get("/books")
    assert response.status_code == 200
    assert len(response.json()) == 2

def test_get_book_by_id(client):
    client.post("/books", json={"title": "Learn FastAPI", "author": "Wizard", "available": True})
    response = client.get("/books/1")
    assert response.status_code == 200
    assert response.json()["title"] == "Learn FastAPI"

def test_get_book_not_found(client):
    response = client.get("/books/999")
    assert response.status_code == 404

def test_update_book(client):
    client.post("/books", json={"title": "Old Title", "author": "Author", "available": True})
    response = client.put("/books/1", json={"title": "New Title"})
    assert response.status_code == 200
    assert response.json()["title"] == "New Title"
    assert response.json()["author"] == "Author"
    assert response.json()["available"] == True

def test_update_book_not_found(client):
    response = client.put("/books/999", json={"title": "New Title"})
    assert response.status_code == 404

def test_delete_book(client):
    client.post("/books", json={"title": "To Delete", "author": "A", "available": True})
    response = client.delete("/books/1")
    assert response.status_code == 200
    assert response.json()["message"] == "Deleted"

def test_delete_book_not_found(client):
    response = client.delete("/books/999")
    assert response.status_code == 404