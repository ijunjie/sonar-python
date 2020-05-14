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
package org.sonar.plugins.python.api.symbols;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.plugins.python.api.LocationInFile;
import org.sonar.plugins.python.api.types.InferredType;

public interface FunctionSymbol extends Symbol {
  List<Parameter> parameters();

  /**
   * When true, it denotes a function symbol for a function stub.
   * <pre>
   *   def fn(p1, p2): ...
   * </pre>
   */
  boolean isStub();

  boolean hasVariadicParameter();

  boolean isInstanceMethod();

  List<String> decorators();

  boolean hasDecorators();

  @CheckForNull
  LocationInFile definitionLocation();

  interface Parameter {
    @CheckForNull
    String name();
    InferredType declaredType();
    boolean hasDefaultValue();
    boolean isVariadic();
    boolean isKeywordOnly();
    boolean isPositionalOnly();
    @CheckForNull
    LocationInFile location();
  }
}
