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
##in Python, defining a function alone does not run it,you need to call the function to run it.
##target is a parameter of the function
##so call the function, and pass a value to it,
result = findtarget(3)
print(result)  # Output: 3

result = findtarget(10)
print(result)  # Output: -1
##########################################################################################

#################global var  eevn if not good practice############
arra = [1,2,3,4,5]
def target(t):
    for i in arra:
        if i == t:
            return i
    return -1

####You must call the function:
print(target(3))  # Output: 3
print(target(10)) # Output: -1
#################################################################

################################################################
def target(arr, t):      # arr is passed as parameter
    for i in arr:
        if i == t:
            return i
    return -1

arr = [1,2,3,4,5]
target_val = 3
print(target(arr, target_val))  # ✅ works
###Here, the function does NOT rely on any external/global variable
###arr is passed as a parameter, so it doesn’t matter where it is defined outside, as long as you pass it when calling the function
###Python only looks at the value at the time of the call, not when the function was defined
#############################################################

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