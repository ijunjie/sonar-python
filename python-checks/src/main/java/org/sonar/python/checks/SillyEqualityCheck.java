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

import java.util.Arrays;
import java.util.HashSet;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.BinaryExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.BuiltinTypes;
import org.sonar.plugins.python.api.types.InferredType;

import static org.sonar.plugins.python.api.types.BuiltinTypes.NONE_TYPE;

@Rule(key = "S2159")
public class SillyEqualityCheck extends PythonSubscriptionCheck {

  private static final HashSet<String> CONSIDERED_OPERATORS = new HashSet<>(Arrays.asList("==", "!="));

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.COMPARISON, ctx -> {
      BinaryExpression binaryExpression = (BinaryExpression) ctx.syntaxNode();
      String operator = binaryExpression.operator().value();
      if (!CONSIDERED_OPERATORS.contains(operator)) {
        return;
      }
      Expression left = binaryExpression.leftOperand();
      Expression right = binaryExpression.rightOperand();
      InferredType leftType = left.type();
      InferredType rightType = right.type();

      if (leftType.isIdentityComparableWith(rightType) || leftType.canOnlyBe(NONE_TYPE) || rightType.canOnlyBe(NONE_TYPE)) {
        return;
      }

      String leftCategory = builtinTypeCategory(leftType);
      String rightCategory = builtinTypeCategory(rightType);
      boolean leftCanImplementEqOrNe = canImplementEqOrNe(left);
      boolean rightCanImplementEqOrNe = canImplementEqOrNe(right);

      if ((!leftCanImplementEqOrNe && !rightCanImplementEqOrNe)
        || (leftCategory != null && rightCategory != null && !leftCategory.equals(rightCategory))
        || (leftCategory != null && !rightCanImplementEqOrNe)
        || (rightCategory != null && !leftCanImplementEqOrNe)) {

        raiseIssue(ctx, binaryExpression, operator);
      }
    });
  }

  private static void raiseIssue(SubscriptionContext ctx, BinaryExpression binaryExpression, String operator) {
    String result = operator.equals("==") ? "False" : "True";
    ctx.addIssue(binaryExpression.operator(), String.format("Remove this equality check between incompatible types; it will always return %s.", result));
  }

  private static boolean canImplementEqOrNe(Expression expression) {
    return expression.type().canHaveMember("__eq__") || expression.type().canHaveMember("__ne__");
  }

  private static String builtinTypeCategory(InferredType inferredType) {
    if (inferredType.canOnlyBe(BuiltinTypes.STR)) {
      return BuiltinTypes.STR;
    }
    if (inferredType.canOnlyBe(BuiltinTypes.INT)
      || inferredType.canOnlyBe(BuiltinTypes.FLOAT)
      || inferredType.canOnlyBe(BuiltinTypes.COMPLEX)
      || inferredType.canOnlyBe(BuiltinTypes.BOOL)) {
      return "number";
    }
    if (inferredType.canOnlyBe(BuiltinTypes.LIST)) {
      return BuiltinTypes.LIST;
    }
    if (inferredType.canOnlyBe(BuiltinTypes.SET)) {
      return BuiltinTypes.SET;
    }
    if (inferredType.canOnlyBe(BuiltinTypes.DICT)) {
      return BuiltinTypes.DICT;
    }
    if (inferredType.canOnlyBe(BuiltinTypes.TUPLE)) {
      return BuiltinTypes.TUPLE;
    }
    return null;
  }
}
