def literals():
  if 42: ... # Noncompliant {{Replace this expression; used as a condition it will always be constant.}}
#    ^^
  if False: ... # Noncompliant
  if 'a string': ... # Noncompliant
  if b'bytes':  ... # Noncompliant
  if {}:  ... # Noncompliant
  if {"a": 1, "b": 2}:  ... # Noncompliant
  if {41, 42, 43}: ... # Noncompliant
  if []:  ... # Noncompliant
  if [41, 42, 43]:  ... # Noncompliant
  if (41, 42, 43):  ... # Noncompliant
  if ():  ... # Noncompliant
  if None:  ... # Noncompliant

def unpacking(p1, p2):
  if ["a string", *p1, *p2]:  ... # Noncompliant
  if [*p1, *p2]: ... # OK, it may be empty or not
  if {"key": 1, **p1, **p2}:  ... # Noncompliant
  if {**p1, **p2}:  ... # OK, it may be empty or not
  if {"key", *p1, *p2}: ... # Noncompliant
  if {*p1, *p2}:  ... # OK, it may be empty or not
  if ("key", *p1, *p2):  ... # Noncompliant
  if (*p1, *p2):  ... # OK, it may be empty or not

def conditional_expr():
  var = 1 if 2 else 3  # Noncompliant

def boolean_expressions():
  if input() or 3:  ... # Noncompliant
#               ^
  if 3 and input():  ... # Noncompliant
  if 3 + input():  ... # OK
  if foo() and bar():  ... # OK
  if not 3:  ... # Noncompliant
  if not input(): ...


  3 + input()
  var = input() or 3  # Ok. 3 does not act as a condition when it is the last value of an "or" chain.
  var = input() and 3  # Ok. 3 does not act as a condition when it is the last value of an "and" chain.


  var = input() and 3 and input()  # Noncompliant
#                   ^
  var = input() or 3 or input()  # Noncompliant
  var = input() and 3 or input()  # Ok. 3 is the return value when the first input() is True.
  var = input() or 3 and input()  # Noncompliant
  var = 3 or input() and input()  # Noncompliant
  var = 3 and input() or input()  # Noncompliant

def ignored():
  # while loops are out of scope
  while True:
    pass
  # builtin constructors are out of scope
  if list():
    pass

def variables(param):
  if param:
    x = 1
  else:
    x = 2

  x = 0
#     ^> {{Last assignment.}}
  if x:  ... # Noncompliant
#    ^

  y = 42
  if y: ... # Noncompliant

def variable_not_constant(param):
  x = param
  if x: ... # OK

  if param:
    z = 1
  else:
    z = 2
  if z: ... # OK

  i = 0
  i += 1
  if i: ...

  for i in []:
    isRunning = False
    if param:
      isRunning = True
    if isRunning: ...


def mutable_variable():
  x = []
  x.append(42)
  if x: ... # FN, x is mutable

#
# Whatever the type of the value assigned to a variable, immutable or not, we don't consider it a constant if either:
# * it is referenced as "nonlocal" in a function we consider that it is not a constant.
# * it is defined in the global scope.
# * if the variable is captured from another scope
#
glob = 42
def nonlocal_reference():
  loc = 0
  def modifying():
    nonlocal loc
    loc = 2
  foo(modifying)
  if loc:  # Ok. loc has been captured as nonlocal by a nested function
    print(loc)

  global glob
  if glob:  # Ok. glob is global
    print(glob)

  loc2 = 1
  def capturing_loc():
    if loc2:  # Ok. loc2 is captured from another scope
      pass

# If a variable with an immutable value is just captured, withut being nonlocal or global, we still consider it a constant.
#
def immutable_captured():
  loc = 1
  def different_variable_with_same_name():
    loc = 2
  different_variable_with_same_name()

  def capturing_without_modifying():
    print(loc + 42)
  capturing_without_modifying()

  if loc:  # Noncompliant
    print(loc)

