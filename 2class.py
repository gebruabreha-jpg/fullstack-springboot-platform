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


DAY22222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222:-
    #Class Variable Power (VERY IMPORTANT 🔑)
    #difference between:-#(Golden Rule):-Instance variables → self.variable and Class variables → ClassName.variable
    #Instance variables (self.name, self.age)
    #Class variables (Student.total_students)
class Student:
    total_student=0
    def __init__(self, name, age,mark):
        self.name=name
        self.age=age
        self.mark=mark
        #Rule:-If something must happen when an object is created, it must be inside __init__
        Student.total_students += 1  # correct place
    
    def display(self):
        print(self.name)
        print(self.age)
        print(self.mark)
ob1=Student("gebru",29,99)
ob2=Student("name",30,95)
ob3=Student("Priya", 19, 72)

#print
print(Student.total_students)

#conculusion:-
#Student.total_students.....shared by all objects
#self.name, self.age, self.mark.... belong to one object only and  __init__ runs every time an object is created

day333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333:-
#Inheritance challenge
    #Inheritance means:Create a new class that reuses another class
    #A CollegeStudent is a Student So it should automatically have:
        #name
        #age
        #mark
    #But a college student also has extra things:
        #course
        #year
#FINAL WORKING CODE (Correct OOP + Inheritance)
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


##/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
# #Note:-
def display(self):
    print(self.name)
    print(self.age)
    print(self.mark)
#Key rule (Python law):
#If a function does not use <return> Python automatically returns None.

#If you want:
    #To store the result
    #To reuse it
    #To test it
#Then rewrite it like this:
def display(self):
    return f"{self.name}, {self.age}, {self.mark}, {self.course}, {self.year}"

#New (clean OOP)
def display(self):
    return {
        "name": self.name,
        "age": self.age,
        "mark": self.mark
    }
#Use __str__ for Human Output
def __str__(self):
    return f"{self.name} | Age: {self.age} | Mark: {self.mark} | {self.result()}"

#Extend, don’t duplicate.
def __str__(self):
    base = super().__str__()
    return f"{base} | {self.course} | Year {self.year}"

#TESTABLE
s = Student("Alex", 20, 35)
assert s.result() == "Failed"
assert s.data()["mark"] == 35

#Validation + Exceptions (VERY IMPORTANT),Never allow an object to be created in an invalid state ,Bad object = bugs everywhere later.
def __init__(self, name, age, mark):
    if age <= 0:
        raise ValueError("Age must be positive")
    if not (0 <= mark <= 100):
        raise ValueError("Mark must be between 0 and 100")
    self.name = name
    self.age = age
    self.mark = mark

#Prefer composition over inheritance unless inheritance is obvious
#Inheritance = tight coupling(CollegeStudent IS A Student ,Use when:Strong relationship and Behavior is shared)
#Composition = flexible design(Student HAS A Address, Student HAS A ReportCard, Use when:You want flexibility and You want to avoid deep inheritance chains)
#Composition
class ReportCard:
    def __init__(self, mark):
        self.mark = mark

    def result(self):
        return "Passed" if self.mark >= 40 else "Failed"
class Student:
    def __init__(self, name, age, report_card):
        self.name = name
        self.age = age
        self.report_card = report_card
rc = ReportCard(75)
s = Student("Alex", 20, rc)
#Student doesn’t need to know HOW marks work — it delegates.
##//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////