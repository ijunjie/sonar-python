def notBasicBuiltinExceptions():
  TypeError() # Noncompliant
# ^^^^^^^^^^^
  Exception("") # Noncompliant {{Throw this exception or remove this useless statement.}}
# ^^^^^^^^^^^^^

# custom class inside same function
def notThrowingCustomException():
  class Custom(TypeError):
    pass

  Custom() # Noncompliant {{Throw this exception or remove this useless statement.}}
# ^^^^^^^^

# custom class outside of any function
class C1(TypeError):
          pass

def customException():
    C1()  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^

def coverage():
  SomethingUnknown()
  SomethingUnknown

  class C2(C3):
    pass

  C2()

def falseNegatives():

  # Deeper inheritance hierarchies currently don't work.
  class C1(TypeError):
    pass

  class C2(C1):
    pass

  C2() # FN. Doesn't report anything, because the inheritance hierarchy of exceptions is currently a linear list,
       # and the isOrExtends is not used.

  # Binders don't bind.
  e = TypeError() # FN. The invocation of the `TypeError` constructor is not a statement, it's an expression.


# rest mutably borrowed from `expected-issues`
class CustomException(TypeError):
  pass

def InstantiatedBuiltinExceptions():
    BaseException()  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^^^^^
    Exception()  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^
    ValueError()  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^^
    CustomException()  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^^^^^^^

    BaseException  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^^^
    Exception  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^
    ValueError  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^
    CustomException  # Noncompliant {{Throw this exception or remove this useless statement.}}
#   ^^^^^^^^^^^^^^^


def compliant(param, func):
    lambda: ValueError() if param else None
    func(ValueError())
    if param == 1:
        raise ValueError() # added constructor invocation (previously no round parens)
    elif param == 2:
        raise ValueError()
    return ValueError()

def gen():
    yield ValueError()
