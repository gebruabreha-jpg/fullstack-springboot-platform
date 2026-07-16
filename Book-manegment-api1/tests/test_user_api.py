import pytest
from fastapi.testclient import TestClient
from app.main import app

def test_create_user(client):
    response = client.post("/users", json={"name": "Test", "email": "test@example.com"})
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == 1
    assert data["name"] == "Test"

def test_get_users(client):
    client.post("/users", json={"name": "User1", "email": "u1@example.com"})
    client.post("/users", json={"name": "User2", "email": "u2@example.com"})
    response = client.get("/users")
    assert response.status_code == 200
    assert len(response.json()) == 2

def test_get_user_by_id(client):
    client.post("/users", json={"name": "Test", "email": "test@example.com"})
    response = client.get("/users/1")
    assert response.status_code == 200
    assert response.json()["name"] == "Test"

def test_get_user_not_found(client):
    response = client.get("/users/999")
    assert response.status_code == 404