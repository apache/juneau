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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.stream.*;

/**
 * One node of the parse tree produced by {@link SearchExpressionParser}: the resolved, validated form of a column's
 * {@code $}-search expression that a context walks to filter rows or build SQL.
 *
 * <p>
 * A node is one of two shapes:
 * <ul>
 * 	<li>A <b>literal</b> ({@link #isLiteral()}): a bare pattern token or a literal argument of a leaf operator
 * 		(for example {@code OPEN} in {@code $eq(OPEN)}).  Its text is {@link #value()}; it has no {@link #args()}.
 * 	<li>A <b>function</b> ({@link #isFunction()}): a {@code $}-operator invocation.  Its {@link #name()} is the
 * 		{@code $}-name, {@link #operator()} is the resolved {@link SearchOperator}, and {@link #args()} are the
 * 		argument nodes &mdash; literal nodes for a leaf operator, or nested function nodes for a combinator.
 * </ul>
 *
 * <p>
 * This tree is the parser's output, not a wire format: a {@link BeanQuery} carries only raw expression strings, and
 * engines call the parser themselves.  See {@link SearchExpressionParser}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SearchExpression <jv>node</jv> = SearchExpressionParser.<jsm>parse</jsm>(<js>"$contains(err)"</js>, SearchOperatorSet.<jsm>standard</jsm>());
 * 	<jk>if</jk> (<jv>node</jv>.isFunction())
 * 		<jsm>evaluate</jsm>(<jv>node</jv>.name(), <jv>node</jv>.literalArgs());  <jc>// name() == "$contains", literalArgs() == ["err"]</jc>
 * </p>
 *
 * <h5 class='section'>Example (typed values):</h5>
 * <p class='bjava'>
 * 	SearchExpression <jv>e</jv> = SearchExpressionParser.<jsm>parse</jsm>(<js>"$eq(42)"</js>, SearchOperatorSet.<jsm>standard</jsm>());
 * 	<jc>// After QueryResolver types it against a NUMERIC column, the arg's typedValue() is a BigDecimal, not "42".</jc>
 * 	Object <jv>arg</jv> = <jv>e</jv>.typedArgs().get(0);
 * </p>
 *
 * @since 10.0.0
 */
public final class SearchExpression {

	private final String name;
	private final String value;
	private final boolean quoted;
	private final SearchOperator operator;
	private final List<SearchExpression> args;
	private final Object typed;

	private SearchExpression(String name, String value, boolean quoted, SearchOperator operator, List<SearchExpression> args, Object typed) {
		this.name = name;
		this.value = value;
		this.quoted = quoted;
		this.operator = operator;
		this.args = args;
		this.typed = typed;
	}

	/** Creates a literal node (a bare pattern token or a leaf argument). */
	static SearchExpression literal(String value, boolean quoted) {
		return new SearchExpression(null, value, quoted, null, List.of(), null);
	}

	/** Creates a function node (a resolved {@code $}-operator invocation). */
	static SearchExpression func(SearchOperator operator, List<SearchExpression> args) {
		return new SearchExpression(operator.name(), null, false, operator, List.copyOf(args), null);
	}

	/**
	 * Whether this is a literal (a bare pattern token or a leaf argument).
	 *
	 * @return <jk>true</jk> if a literal.
	 */
	public boolean isLiteral() {
		return name == null;
	}

	/**
	 * Whether this is a {@code $}-operator invocation.
	 *
	 * @return <jk>true</jk> if a function.
	 */
	public boolean isFunction() {
		return name != null;
	}

	/**
	 * The operator {@code $}-name, or <jk>null</jk> for a literal.
	 *
	 * @return The name, or <jk>null</jk>.
	 */
	public String name() {
		return name;
	}

	/**
	 * The literal text, or <jk>null</jk> for a function.
	 *
	 * @return The value, or <jk>null</jk>.
	 */
	public String value() {
		return value;
	}

	/**
	 * Whether this literal was written as a quoted value ({@code "a*"} or {@code 'a*'}) in the search string.
	 *
	 * <p>
	 * A quoted literal is never wildcard-interpreted: {@code name="a*"} matches the literal text {@code a*}
	 * (case-insensitive substring, like any bare value without wildcards), while {@code name=a*} treats {@code *} as
	 * a wildcard. Always <jk>false</jk> for a function node.
	 *
	 * @return <jk>true</jk> if this literal was quoted in the source search string.
	 */
	public boolean isQuoted() {
		return quoted;
	}

	/**
	 * The resolved operator, or <jk>null</jk> for a literal.
	 *
	 * @return The operator, or <jk>null</jk>.
	 */
	public SearchOperator operator() {
		return operator;
	}

	/**
	 * The argument nodes (empty for a literal or a zero-arg function such as {@code $blank()}).
	 *
	 * @return An unmodifiable list of argument nodes.
	 */
	public List<SearchExpression> args() {
		return args;
	}

	/**
	 * The number of argument nodes.
	 *
	 * @return The argument count.
	 */
	public int argCount() {
		return args.size();
	}

	/**
	 * This function's arguments as literal strings &mdash; the shape a leaf operator (built-in or custom) consumes.
	 *
	 * <p>
	 * A non-literal (nested function) argument contributes its {@code $}-name so the list length always equals
	 * {@link #argCount()}.
	 *
	 * @return An unmodifiable list of the literal argument strings.
	 */
	public List<String> literalArgs() {
		return args.stream().map(x -> x.isLiteral() ? x.value : x.name).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
	}

	/**
	 * The parsed, type-aware value for this node.
	 *
	 * <p>
	 * For a literal node that {@link QueryResolver} typed (a bare literal, or an argument of a value operator such
	 * as {@code $eq}/{@code $in}/{@code $gt}/{@code $between}), this is the {@link SearchType#parse(String) parsed}
	 * object ({@link java.math.BigDecimal}, {@link Boolean}, or a {@code java.time} type).  For every other node
	 * (untyped literals, function nodes) this returns {@link #value()} unchanged, so callers can read
	 * {@code typedValue()} instead of {@code value()} without a null check.
	 *
	 * @return The typed value, or the literal text.
	 */
	public Object typedValue() {
		return typed != null ? typed : value;
	}

	/**
	 * Returns a copy of this literal node carrying a parsed value, leaving this node unchanged.
	 *
	 * <p>
	 * Called only by {@link QueryResolver}, the sole place a raw string is known to belong to a specific
	 * {@link SearchType}.
	 *
	 * @param value The parsed value.  Must not be <jk>null</jk>.
	 * @throws IllegalStateException If this is a function node.
	 * @return A new node equal to this one except for {@link #typedValue()}.
	 */
	SearchExpression withTypedValue(Object value) {
		if (isFunction())
			throw new IllegalStateException("Cannot attach a typed value to function node '" + name + "'.");
		return new SearchExpression(name, this.value, quoted, operator, args, Objects.requireNonNull(value, "value"));
	}

	/**
	 * This function's arguments' {@link #typedValue() typed values}, in order - the shape a value operator, or a
	 * custom operator opting into {@link SearchOperator.Builder#typedArgs(boolean) typedArgs(true)}, consumes instead
	 * of {@link #literalArgs()}.
	 *
	 * <p>
	 * A nested function argument contributes its {@code $}-name, exactly as in {@link #literalArgs()}, so the list
	 * never contains <jk>null</jk> for a function argument and its length always equals {@link #argCount()}.
	 *
	 * @return An unmodifiable list of the typed argument values, one per {@link #args()} entry.
	 */
	public List<Object> typedArgs() {
		return args.stream().map(x -> x.isLiteral() ? x.typedValue() : (Object)x.name).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (! (o instanceof SearchExpression other))
			return false;
		return quoted == other.quoted && eq(name, other.name) && eq(value, other.value) && eq(operator, other.operator)
			&& eq(args, other.args) && eq(typed, other.typed);
	}

	@Override /* Object */
	public int hashCode() {
		return h(name, value, quoted, operator, args, typed);
	}

	@Override /* Object */
	public String toString() {
		return isLiteral() ? value : (name + "(" + args.stream().map(SearchExpression::toString).collect(Collectors.joining(",")) + ")");
	}
}
