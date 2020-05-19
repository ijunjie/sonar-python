def basic_calls():
    round(1.3) # Noncompliant {{The return value of "round" must be used.}}
   #^^^^^
    list([1, 2])  # Noncompliant
    x = round(1.3)
    x = y = round(1.3)
    round(1.3) + round(1.2)
    round(1.2), round(1.3) # Noncompliant 2
    print(x) # Ok
    no_such_function(1)

def string_calls():
    "hello".capitalize() # Noncompliant
    s0 = "hello".capitalize()
    s1 = "hello"
    s1.capitalize() # Noncompliant
    s2 = s1.capitalize()
    s2[0] # Noncompliant
    'll' in s2 # Noncompliant

from collections import defaultdict

class MyDict(dict):
    pass

class MyCollection:
    def __contains__(self, item):
        pass

def collection():
    dict({'a': 1, 'b': 2}) # Noncompliant
    d1 = dict({'a': 1, 'b': 2})
    d1.copy() # Noncompliant

    d1['a'] # Noncompliant {{The return value of "__getitem__" must be used.}}
   #^^^^^^^
    'a' in d1 # Noncompliant {{The return value of "__contains__" must be used.}}
   #^^^^^^^^^
    x = d1['a']
    x = 'a' in d1

    d2 = defaultdict()
    d2['a'] # Ok - defaultdict.__geitem__ creates the key if missing
    d3 = MyDict()
    d3['a'] # Noncompliant
    foo = MyCollection
    'a' in foo # Ok

def calling_instance_methods_via_string():
    # Calling instance methods on the class has no side effect either
    str.islower('this is passed as self')  # Noncompliant

def exceptions_in_try_blocks():
    try:
        int("abc") # Ok
    except ValueError as e:
        int("abc")  # Noncompliant

    try:
        int("abc") # Ok
        return
    except ValueError as e:
        pass

    # We only allow the above behavior if there are only 2 statement in the try block
    try:
        int("abc") # Noncompliant
        int("abc") # Noncompliant
        return
    except ValueError as e:
        pass

    try:
        int("abc")  # Ok
        int("cde")  # Ok
    except ValueError as e:
        pass

    try:
        x = int("abc")  # Ok
        y - int("cde")  # Ok
    except ValueError as e:
        pass

def edge_case():
    round = 1
    round(1.3)
