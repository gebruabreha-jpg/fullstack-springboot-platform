from logic import TaskManager

def test_task_manager():
    tm = TaskManager()
    
    task1 = tm.add_task("Buy groceries")
    assert task1.id == 1
    assert task1.title == "Buy groceries"
    assert task1.done == False
    
    task2 = tm.add_task("Write tests")
    assert task2.id == 2
    
    assert len(tm.list_tasks()) == 2
    
    found = tm.get_task(1)
    assert found.title == "Buy groceries"
    
    assert tm.get_task(999) is None
    
    tm.update_task(1, done=True)
    assert tm.get_task(1).done == True
    
    tm.update_task(2, title="Write unit tests")
    assert tm.get_task(2).title == "Write unit tests"
    
    assert tm.delete_task(1) == True
    assert tm.get_task(1) is None
    assert tm.delete_task(999) == False
    
    print("All tests passed")

if __name__ == "__main__":
    test_task_manager()