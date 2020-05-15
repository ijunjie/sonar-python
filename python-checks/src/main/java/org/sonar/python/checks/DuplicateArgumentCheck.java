/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.LocationInFile;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.ClassSymbol;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.Argument;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.DictionaryLiteral;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.KeyValuePair;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.RegularArgument;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.UnpackingExpression;
import org.sonar.python.api.PythonPunctuator;
import org.sonar.python.semantic.FunctionSymbolImpl;
import org.sonar.python.semantic.SymbolUtils;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S5549")
public class DuplicateArgumentCheck extends PythonSubscriptionCheck {

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, ctx -> {
      CallExpression callExpression = (CallExpression) ctx.syntaxNode();
      Symbol symbol = callExpression.calleeSymbol();
      if (symbol == null || !symbol.is(Symbol.Kind.FUNCTION)) {
        return;
      }
      FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
      boolean isStaticCall = false;
      if (callExpression.callee().is(Tree.Kind.QUALIFIED_EXPR)) {
        QualifiedExpression qualifiedExpression = (QualifiedExpression) callExpression.callee();
        isStaticCall = TreeUtils.getSymbolFromTree(qualifiedExpression.qualifier())
          .filter(s -> s.is(Symbol.Kind.CLASS))
          .isPresent();
      }
      int firstParameterOffset = SymbolUtils.firstParameterOffset(functionSymbol, isStaticCall);
      if (isException(functionSymbol) || firstParameterOffset == -1) {
        return;
      }
      checkFunctionCall(callExpression, functionSymbol, firstParameterOffset, ctx);
    });
  }

  private static void checkFunctionCall(CallExpression callExpression, FunctionSymbol functionSymbol, int firstParameterOffset, SubscriptionContext ctx) {
    Map<String, List<Tree>> passedParameters = new HashMap<>();
    List<FunctionSymbol.Parameter> parameters = functionSymbol.parameters();
    List<Argument> arguments = callExpression.arguments();
    for (int i = 0; i < arguments.size(); i++) {
      Argument argument = arguments.get(i);
      if (argument.is(Tree.Kind.REGULAR_ARGUMENT)) {
        RegularArgument regularArgument = (RegularArgument) argument;
        int parameterIndex = i + firstParameterOffset;
        boolean shouldAbortCheck = checkRegularArgument(regularArgument, parameters, parameterIndex, passedParameters);
        if (shouldAbortCheck) {
          return;
        }
      } else {
        UnpackingExpression unpackingExpression = (UnpackingExpression) argument;
        boolean isDictionary = unpackingExpression.starToken().type().equals(PythonPunctuator.MUL_MUL);
        if (isDictionary) {
          checkDictionary(unpackingExpression, passedParameters);
        } else {
          // No issue raised for unpacked positional arguments
          return;
        }
      }
    }
    reportIssues(passedParameters, functionSymbol, ctx);
  }

  /**
   * Returns true if check should be aborted without raising issues to avoid FPs
   **/
  private static boolean checkRegularArgument(RegularArgument regularArgument, List<FunctionSymbol.Parameter> parameters, int parameterIndex,
                                              Map<String, List<Tree>> passedParameters) {
    Name keyword = regularArgument.keywordArgument();
    if (keyword == null) {
      if (parameterIndex >= parameters.size()) {
        return false;
      }
      FunctionSymbol.Parameter parameter = parameters.get(parameterIndex);
      if (parameter.name() == null || parameter.isVariadic()) {
        // Avoid FPs in case of tuple or variadic parameter that could capture unexpected arguments
        return true;
      }
      if (!parameter.isPositionalOnly()) {
        // Positional only parameters cannot be passed twice.
        // Unexpected keyword arguments should be handled by S930.
        passedParameters.computeIfAbsent(parameter.name(), k -> new ArrayList<>()).add(regularArgument);
      }
    } else {
      passedParameters.computeIfAbsent(keyword.name(), k -> new ArrayList<>()).add(regularArgument);
    }
    return false;
  }

  private static void checkDictionary(UnpackingExpression unpackingExpression, Map<String, List<Tree>> passedParameters) {
    List<String> dictionaryKeys = extractKeysFromDictionary(unpackingExpression);
    for (String key : dictionaryKeys) {
      passedParameters.computeIfAbsent(key, k -> new ArrayList<>()).add(unpackingExpression);
    }
  }

  private static void reportIssues(Map<String, List<Tree>> passedParameters, FunctionSymbol functionSymbol, SubscriptionContext ctx) {
    passedParameters.forEach((key, list) -> {
      if (list.size() > 1) {
        PreciseIssue issue = ctx.addIssue(list.get(0), String.format("Remove duplicate values for parameter \"%s\" in \"%s\" call.", key, functionSymbol.name()));
        list.stream().skip(1).forEach(t -> issue.secondary(t, "Argument is also passed here."));
        LocationInFile locationInFile = functionSymbol.definitionLocation();
        if (locationInFile != null) {
          issue.secondary(locationInFile, "Function definition.");
        }
      }
    });
  }

  private static List<String> extractKeysFromDictionary(UnpackingExpression unpackingExpression) {
    if (unpackingExpression.expression().is(Tree.Kind.DICTIONARY_LITERAL)) {
      return keysInDictionaryLiteral((DictionaryLiteral) unpackingExpression.expression());
    } else if (unpackingExpression.expression().is(Tree.Kind.CALL_EXPR)) {
      return keysFromDictionaryCreation((CallExpression) unpackingExpression.expression());
    } else if (unpackingExpression.expression().is(Tree.Kind.NAME)) {
      Name name = (Name) unpackingExpression.expression();
      Symbol symbol = name.symbol();
      if (symbol == null || symbol.usages().stream().anyMatch(u -> TreeUtils.firstAncestorOfKind(u.tree(), Tree.Kind.DEL_STMT) != null)) {
        return Collections.emptyList();
      }
      Expression expression = Expressions.singleAssignedValue(name);
      if (expression != null && expression.is(Tree.Kind.CALL_EXPR)) {
        return keysFromDictionaryCreation((CallExpression) expression);
      }
      return expression != null && expression.is(Tree.Kind.DICTIONARY_LITERAL) ? keysInDictionaryLiteral((DictionaryLiteral) expression) : Collections.emptyList();
    }
    return Collections.emptyList();
  }

  private static List<String> keysFromDictionaryCreation(CallExpression callExpression) {
    Symbol calleeSymbol = callExpression.calleeSymbol();
    if (calleeSymbol != null && "dict".equals(calleeSymbol.fullyQualifiedName())) {
      return callExpression.arguments().stream()
        .filter(a -> a.is(Tree.Kind.REGULAR_ARGUMENT) && ((RegularArgument) a).keywordArgument() != null)
        .map(a -> ((RegularArgument) a).keywordArgument().name())
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private static List<String> keysInDictionaryLiteral(DictionaryLiteral dictionaryLiteral) {
    return dictionaryLiteral.elements().stream()
      .filter(e -> e.is(Tree.Kind.KEY_VALUE_PAIR))
      .map(kv -> ((KeyValuePair) kv).key())
      .filter(k -> k.is(Tree.Kind.STRING_LITERAL))
      .map(s -> ((StringLiteral) s).trimmedQuotesValue())
      .collect(Collectors.toList());
  }

  private static boolean isException(FunctionSymbol functionSymbol) {
    return functionSymbol.hasDecorators() || extendsZopeInterface(((FunctionSymbolImpl) functionSymbol).owner());
  }

  private static boolean extendsZopeInterface(@Nullable Symbol symbol) {
    if (symbol != null && symbol.is(Symbol.Kind.CLASS)) {
      return ((ClassSymbol) symbol).isOrExtends("zope.interface.Interface");
    }
    return false;
  }
}
