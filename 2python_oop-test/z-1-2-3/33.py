DAY11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111:-
#creat class with 3 var
from symtable import Class
from unicodedata import name
from traitlets import Instance
class Student:
    #Create a constructor (__init__) to initialize these values.
    def __init__(self, name, age, mark):
        self.name= name
        self.age= age
        self.mark= mark
    #Create another method called is_passed().
    def display(self):
        print(self.name)
        print(self.age)
        print(self.mark)

    #Create another method called is_passed()
    def is_passed(self):
        if self.age > 20:
            return self.age
        else:
            return self.mark
# Create one object of the class and call both methods
obj1 = Student("Alex", 22, 80)
meth1 =obj1.display()
meth2 =obj1.is_passed()
print(meth1)
print(meth2)

# Creating another object of Student
student1 = Student("Rahul", 12, 65)
student1.display()
student1.is_passed()
print(student1.display())
print(student1.is_passed())
#############################################################################################
class Student:
    total_students = 0

    def __init__(self, name, age, mark):
        self.name = name
        self.age = age
        self.mark = mark
        Student.total_students += 1

    def display(self):
        print(self.name)
        print(self.age)
        print(self.mark)

class student:
    def__init__(self, name,age, mark):
        self.name=name
        self.age=age
        self.mark=mark
    def display(self):
        print(self.name)
        print(self.age)
        print(self.mark)
        


class CollegeStudent(Student):
    def __init__(self, name, age, mark, course, year):
        # let Student handle name, age, mark
        super().__init__(name, age, mark)

        # CollegeStudent-specific data
        self.course = course
        self.year = year

    def display(self):
        # reuse Student display
        super().display()

        # extend with new info
        print(self.course)
        print(self.year)
# ---------- Testing ----------
cs1 = CollegeStudent("Priya", 19, 72, "BSc", 2)
cs1.display()
print("Total students:", Student.total_students)

###################################################################################
class Student:
    total_students = 0

    def __init__(self, name, age, mark):
        self.name = name
        self.age = age
        self.mark = mark
        Student.total_students += 1

    def display(self):
        print(self.name)
        print(self.age)
        print(self.mark)


class CollegeStudent(Student):
    def __init__(self, name, age, mark, course, year):
        # let Student handle name, age, mark
        super().__init__(name, age, mark)

        # CollegeStudent-specific data
        self.course = course
        self.year = year

    def display(self):
        # reuse Student display
        super().display()

        # extend with new info
        print(self.course)
        print(self.year)
# ---------- Testing ----------
cs1 = CollegeStudent("Priya", 19, 72, "BSc", 2)
cs1.display()
print("Total students:", Student.total_students)
