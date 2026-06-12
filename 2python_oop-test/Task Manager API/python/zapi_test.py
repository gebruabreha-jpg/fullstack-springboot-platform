from fastapi.testclient import TestClient
from api import app, tm

client = TestClient(app)

def test_create_and_get_tasks():
    rv = client.post('/tasks', json={'title': 'Test task'})
    assert rv.status_code == 201
    data = rv.json()
    assert data['title'] == 'Test task'
    assert data['done'] == False

    rv = client.get('/tasks')
    assert rv.status_code == 200
    assert len(rv.json()) == 1

def test_get_single_task():
    tm.tasks.clear()
    tm.counter = 1
    rv = client.post('/tasks', json={'title': 'Task 1'})
    rv = client.get('/tasks/1')
    assert rv.status_code == 200
    assert rv.json()['title'] == 'Task 1'

    rv = client.get('/tasks/999')
    assert rv.status_code == 404

def test_update_task():
    tm.tasks.clear()
    tm.counter = 1
    client.post('/tasks', json={'title': 'Original'})
    rv = client.put('/tasks/1', json={'done': True})
    assert rv.status_code == 200
    assert rv.json()['done'] == True

def test_delete_task():
    tm.tasks.clear()
    tm.counter = 1
    client.post('/tasks', json={'title': 'To delete'})
    rv = client.delete('/tasks/1')
    assert rv.status_code == 204
    rv = client.get('/tasks/1')
    assert rv.status_code == 404