#,Python uses lists as dynamic arrays,Operations: Access, Insert, Delete, Iterate.
#method1:-Cons: Cannot return the index of the element (only the value).
#############################################################################
def findtarget(target):
    mylist=[1,2,3,4,5,6]
    for i in mylist:
        if i==target:
            return i
    else:
        return -1
result = findtarget(3)
print(result)  # Output: 3
##########################################################################################

#################global var  eevn if not good practice############
arra = [1,2,3,4,5]
def target(t):
    for i in arra:
        if i == t:
            return i
    return -1
print(target(3))  # Output: 3
print(target(10)) # Output: -1
#################################################################
this is not a global variable method.
What you wrote is a function with parameters, not a global-variable approach.
def target(arr, t):
    for i in range(len(arr)):
        if i==t:
            return i
    return None
arr =[1,2,3,5,6]
t=3
resualt=target(arr,t)
print(resualt)
#####################################################################
def findtarget(target):
    mylist = [1, 2, 3, 4, 5, 6]
    for i in range(len(mylist)):  # i is index
        if mylist[i] == target:
            return i  # return the index instead of value
    return -1

print(findtarget(4))   # 3 → index of 4
print(findtarget(10))  # -1 → not found
##########################enum###############################################3
#use enum which Gives both index and value in a clean way.
#Avoids manually writing range(len(mylist)).
def findtarget(target):
    mylist = [1, 2, 3, 4, 5, 6]
    for index, value in enumerate(mylist):
        if value == target:
            return index  # returns index of the target
    return -1  # if target not found

print(findtarget(4))   # 3 → index of 4
print(findtarget(10))  # -1 → not found
############################################################################################