/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.svl;

import java.util.*;
import java.util.stream.*;

/**
 * Abstract syntax tree of an SVL <c>#{...}</c> expression.
 *
 * <p>
 * Every node carries the source offset ({@code pos}) it started at, as its last record component.
 */
sealed interface Expr {

	/** @return The source offset of this node. */
	int pos();

	/** @return A compact S-expression rendering of this tree (used by parser tests). */
	String show();

	/** A literal value (string, number, boolean or <jk>null</jk>). @param v The value. @param pos Source offset. */
	record Literal(Object v, int pos) implements Expr {
		@Override public String show() {
			return v instanceof String s ? "'" + s.replace("'", "''") + "'" : String.valueOf(v);
		}
	}

	/** A <c>${...}</c> reference. @param raw Token text including the braces. @param pos Source offset. */
	record VarRef(String raw, int pos) implements Expr {
		@Override public String show() { return raw; }
	}

	/** A bare identifier (scope lookup). @param name The name. @param pos Source offset. */
	record Ident(String name, int pos) implements Expr {
		@Override public String show() { return name; }
	}

	/** A function call. @param fn Function name. @param args Arguments. @param pos Source offset. */
	record Call(String fn, List<Expr> args, int pos) implements Expr {
		@Override public String show() {
			return "(" + fn + args.stream().map(a -> " " + a.show()).collect(Collectors.joining()) + ")";
		}
	}

	/** A prefix operator. @param op Operator (<c>not</c>, <c>-</c>, <c>+</c>). @param e Operand. @param pos Source offset. */
	record Unary(String op, Expr e, int pos) implements Expr {
		@Override public String show() { return "(" + op + " " + e.show() + ")"; }
	}

	/** An infix operator. @param op Operator. @param l Left operand. @param r Right operand. @param pos Source offset. */
	record Binary(String op, Expr l, Expr r, int pos) implements Expr {
		@Override public String show() { return "(" + op + " " + l.show() + " " + r.show() + ")"; }
	}

	/** A ternary conditional. @param c Condition. @param a Then branch. @param b Else branch. @param pos Source offset. */
	record Ternary(Expr c, Expr a, Expr b, int pos) implements Expr {
		@Override public String show() { return "(? " + c.show() + " " + a.show() + " " + b.show() + ")"; }
	}

	/** An Elvis operator. @param l Left operand. @param r Fallback. @param pos Source offset. */
	record Elvis(Expr l, Expr r, int pos) implements Expr {
		@Override public String show() { return "(?: " + l.show() + " " + r.show() + ")"; }
	}

	/** A property access. @param target Target. @param name Property name. @param nullSafe Whether <c>?.</c> was used. @param pos Source offset. */
	record Path(Expr target, String name, boolean nullSafe, int pos) implements Expr {
		@Override public String show() { return "(" + (nullSafe ? "?." : ".") + " " + target.show() + " " + name + ")"; }
	}

	/** An index access. @param target Target. @param idx Index expression. @param nullSafe Whether the access is null-safe. @param pos Source offset. */
	record Index(Expr target, Expr idx, boolean nullSafe, int pos) implements Expr {
		@Override public String show() { return "(" + (nullSafe ? "?idx" : "idx") + " " + target.show() + " " + idx.show() + ")"; }
	}

	/** A collection selection <c>.?[pred]</c>. @param target Target. @param pred Predicate. @param pos Source offset. */
	record Select(Expr target, Expr pred, int pos) implements Expr {
		@Override public String show() { return "(.? " + target.show() + " " + pred.show() + ")"; }
	}

	/** A collection projection <c>.![expr]</c>. @param target Target. @param map Mapping expression. @param pos Source offset. */
	record Project(Expr target, Expr map, int pos) implements Expr {
		@Override public String show() { return "(.! " + target.show() + " " + map.show() + ")"; }
	}
}
