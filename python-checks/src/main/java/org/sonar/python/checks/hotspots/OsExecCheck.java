/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
package org.sonar.python.checks.hotspots;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import java.util.Collections;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.python.PythonCheck;
import org.sonar.python.api.PythonGrammar;
import org.sonar.python.semantic.Symbol;

@Rule(key = OsExecCheck.CHECK_KEY)
public class OsExecCheck extends PythonCheck {

  public static final String CHECK_KEY = "S4721";
  private static final String MESSAGE = "Make sure that executing this OS command is safe here.";

  private static final Set<String> questionableFunctions = immutableSet("subprocess.run",
    "subprocess.Popen",
    "subprocess.call",
    "subprocess.check_call",
    "subprocess.check_output",
    "subprocess.getstatusoutput",
    "subprocess.getoutput",
    "os.system",
    "os.spawnl",
    "os.spawnle",
    "os.spawnlp",
    "os.spawnlpe",
    "os.spawnv",
    "os.spawnve",
    "os.spawnvp",
    "os.spawnvpe",
    "os.popen",
    "os.startfile",
    "os.execl",
    "os.execle",
    "os.execlp",
    "os.execlpe",
    "os.execv",
    "os.execve",
    "os.execvp",
    "os.execvpe",
    "os.popen2",
    "os.popen3",
    "os.popen4",
    "popen2.popen2",
    "popen2.popen3",
    "popen2.popen4"
  );

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Collections.singleton(PythonGrammar.CALL_EXPR);
  }


  @Override
  public void visitNode(AstNode node) {
    Symbol symbol = getContext().symbolTable().getSymbol(node);
    if (symbol != null && questionableFunctions.contains(symbol.qualifiedName())) {
      addIssue(node, MESSAGE);
    }
  }
}