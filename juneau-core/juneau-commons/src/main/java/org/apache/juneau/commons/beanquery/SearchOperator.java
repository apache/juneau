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

/**
 * The immutable definition of a single {@code $}-search operator &mdash; its {@code $}-name, arity, the value types
 * it applies to, and its popup help text.
 *
 * <p>
 * Every operator, built-in ({@link SearchOperatorSet#standard()}) or custom, carries its own help text on this one
 * API.  A custom operator additionally may carry a {@link SearchPredicate} the in-memory context calls when it hits
 * that {@code $}-name &mdash; Juneau never invents one.  Anything else a downstream module (for example a SQL
 * renderer) needs to attach lives in the typed {@link #extension(Class) extension} slot: it is keyed by the
 * extension's own {@link Class}, so this module (commons) never needs to depend on that module's types (design
 * §5.3).
 *
 * <p>
 * Instances are built with {@link Builder}: {@link #create(String, String)} starts a new custom operator, and
 * {@link #copy()} starts a modified copy of an existing one (built-in or custom).
 *
 * <h5 class='section'>Combinators vs. leaves</h5>
 * <p>
 * {@code $and}/{@code $or}/{@code $not} are {@link #isCombinator() combinators}: their children are sub-expressions,
 * not literal value arguments, and they apply to every type.  Every other operator is a leaf whose arguments are
 * literal values and whose {@link #appliesTo(SearchType) applicability} is a fixed type set (an <b>empty</b> type set
 * means "applies to all types").
 *
 * <h5 class='section'>Example (custom operator, with a typed extension):</h5>
 * <p class='bjava'>
 * 	SearchOperator <jv>op</jv> = SearchOperator.<jsm>create</jsm>(<js>"$near"</js>, <js>"Fuzzy match. Example: $near(jhon)"</js>)
 * 		.types(SearchType.<jsf>TEXT</jsf>)
 * 		.predicate((<jv>cell</jv>, <jv>args</jv>) -> <jsm>fuzzy</jsm>(<jv>cell</jv>, <jv>args</jv>.get(0)))
 * 		.extension(MySqlRenderer.<jk>class</jk>, <jv>myRenderer</jv>)
 * 		.build();
 *
 * 	<jc>// A later consumer that knows about MySqlRenderer retrieves it back, type-safe, with no cast.</jc>
 * 	Optional&lt;MySqlRenderer&gt; <jv>renderer</jv> = <jv>op</jv>.extension(MySqlRenderer.<jk>class</jk>);
 * </p>
 *
 * @since 10.0.0
 */
public final class SearchOperator {

	private final String name;
	private final String help;
	private final int minArgs;
	private final int maxArgs;
	private final Set<SearchType> types;
	private final SearchPredicate predicate;
	private final boolean custom;
	private final boolean combinator;
	private final boolean typedArgs;
	private final Map<Class<?>,Object> extensions;

	private SearchOperator(Builder b) {
		name = b.name;
		help = b.help;
		minArgs = b.minArgs;
		maxArgs = b.maxArgs;
		types = Set.copyOf(b.types);
		predicate = b.predicate;
		custom = b.custom;
		combinator = b.combinator;
		typedArgs = b.typedArgs;
		extensions = u(new LinkedHashMap<>(b.extensions));
	}

	/**
	 * Creates a built-in operator definition.  Package-private &mdash; the canonical set is
	 * {@link SearchOperatorSet#standard()}; applications add operators through {@link #create(String, String)}.
	 */
	static SearchOperator builtin(String name, int minArgs, int maxArgs, boolean combinator, String help, SearchType...types) {
		var b = new Builder();
		b.name = name;
		b.help = help;
		b.minArgs = minArgs;
		b.maxArgs = maxArgs;
		b.custom = false;
		b.combinator = combinator;
		b.types = types == null || types.length == 0 ? new LinkedHashSet<>() : new LinkedHashSet<>(l(types));
		return b.build();
	}

	/**
	 * Starts a new <b>custom</b> (application-defined) leaf operator.
	 *
	 * <p>
	 * Defaults: arity {@code 1..*} (one or more arguments), applies to <b>all</b> value types (an empty type set),
	 * no predicate, no extensions.  Narrow it with {@link Builder#minArgs(int)}/{@link Builder#maxArgs(int)}/
	 * {@link Builder#types(SearchType...)} and attach an evaluator with {@link Builder#predicate(SearchPredicate)}
	 * (in-memory) and/or a typed {@link Builder#extension(Class, Object)} (for example, a SQL renderer).
	 *
	 * @param name The operator name, including the leading {@code $} (e.g. {@code "$near"}).  Must not be
	 * 	<jk>null</jk> or blank and must start with {@code $}.
	 * @param helpText The popup help text: what the operator accepts and a short example.  Must not be
	 * 	<jk>null</jk> or blank.
	 * @return A new {@link Builder} pre-filled with {@code name}/{@code helpText} and the defaults above.
	 * @throws IllegalArgumentException If {@code name} is not a valid {@code $}-name or {@code helpText} is blank.
	 */
	public static Builder create(String name, String helpText) {
		req(inb(name) && name.startsWith("$") && name.length() >= 2,
			"SearchOperator name must start with '$' and not be blank: '%s'", name);
		req(inb(helpText), "SearchOperator help text must not be null or blank (operator '%s').", name);
		var b = new Builder();
		b.name = name;
		b.help = helpText;
		b.custom = true;
		return b;
	}

	/**
	 * Starts a {@link Builder} pre-filled with this operator's current state, for building a modified copy.
	 *
	 * <p>
	 * The returned builder is fully independent of this instance: mutating it (or the {@link SearchOperator} it
	 * eventually {@link Builder#build() builds}) never affects this instance.
	 *
	 * <p>
	 * <b>Renaming is only legal for a copy of a <i>custom</i> operator.</b>  Both evaluation engines dispatch a
	 * built-in purely by its {@link #name()} &mdash; the in-memory engine's leaf switch and
	 * {@code SqlDialect#builtinRenderer(String)} alike &mdash; so {@link Builder#name(String) changing the name} on a
	 * copy of a <i>custom</i> operator (one with its own {@link Builder#predicate(SearchPredicate) predicate}/
	 * {@link Builder#extension(Class, Object) extension}) is fine and gives it a fresh identity, but doing the same
	 * to a copy of a <b>built-in</b> operator throws {@link IllegalArgumentException} from {@link Builder#build()
	 * build()}.  A built-in's copy may still have its {@link Builder#help(String) help text} changed freely; only its
	 * name is fixed.
	 *
	 * @return A new builder seeded from this operator.
	 */
	public Builder copy() {
		var b = new Builder();
		b.name = name;
		b.help = help;
		b.minArgs = minArgs;
		b.maxArgs = maxArgs;
		b.types = new LinkedHashSet<>(types);
		b.predicate = predicate;
		b.custom = custom;
		b.combinator = combinator;
		b.typedArgs = typedArgs;
		b.extensions = new LinkedHashMap<>(extensions);
		if (! custom)
			b.builtinOriginalName = name;
		return b;
	}

	/**
	 * The operator name, including the leading {@code $} (e.g. {@code "$eq"}).
	 *
	 * @return The operator name.
	 */
	public String name() {
		return name;
	}

	/**
	 * The popup help text shown under the value box.
	 *
	 * @return The help text.
	 */
	public String help() {
		return help;
	}

	/**
	 * Whether this is an application-defined custom operator (vs. a built-in).
	 *
	 * @return <jk>true</jk> if custom.
	 */
	public boolean isCustom() {
		return custom;
	}

	/**
	 * Whether this is a combinator ({@code $and}/{@code $or}/{@code $not}) whose children are sub-expressions.
	 *
	 * @return <jk>true</jk> if a combinator.
	 */
	public boolean isCombinator() {
		return combinator;
	}

	/**
	 * The minimum argument count (inclusive).
	 *
	 * @return The minimum arity.
	 */
	public int minArgs() {
		return minArgs;
	}

	/**
	 * The maximum argument count (inclusive), or {@code -1} for unbounded.
	 *
	 * @return The maximum arity, or {@code -1}.
	 */
	public int maxArgs() {
		return maxArgs;
	}

	/**
	 * The in-memory evaluator for a custom operator, if one was supplied.
	 *
	 * @return The predicate, or <jk>null</jk> for built-ins and for customs with no predicate.
	 */
	public SearchPredicate predicate() {
		return predicate;
	}

	/**
	 * The value types this operator applies to (combinators apply to all types regardless of this set).
	 *
	 * @return An unmodifiable view of the applicable types; empty means "applies to all types".
	 */
	public Set<SearchType> types() {
		return types;
	}

	/**
	 * Whether this operator applies to the specified value type.
	 *
	 * @param type The column value type.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if this operator is offered for that type (combinators, and operators with an empty
	 * 	{@link #types() type set}, apply to every type).
	 */
	public boolean appliesTo(SearchType type) {
		return type != null && (combinator || types.isEmpty() || types.contains(type));
	}

	/**
	 * Whether the specified argument count satisfies this operator's arity.
	 *
	 * @param count The number of arguments supplied.
	 * @return <jk>true</jk> if {@code count} is within {@code [minArgs, maxArgs]} (upper bound ignored when unbounded).
	 */
	public boolean acceptsArgCount(int count) {
		return count >= minArgs && (maxArgs < 0 || count <= maxArgs);
	}

	/**
	 * Whether this operator's SQL renderer should be given {@link SearchExpression#typedArgs() typed values} rather
	 * than {@link SearchExpression#literalArgs() literal strings}.  Never affects the in-memory
	 * {@link #predicate() predicate}, which always receives literal strings.
	 *
	 * @return <jk>true</jk> if typed (see {@link Builder#typedArgs(boolean)}).
	 */
	public boolean typedArgs() {
		return typedArgs;
	}

	/**
	 * Looks up a typed extension previously attached with {@link Builder#extension(Class, Object)}.
	 *
	 * <p>
	 * This is how a downstream module (for example {@code juneau-beanquery-sql}'s SQL renderer) attaches its own
	 * metadata to a custom operator without this module (commons) ever depending on that module's types: the
	 * extension is keyed by its own {@link Class}, so retrieval is type-safe with no cast (design §5.3).
	 *
	 * @param <X> The extension type.
	 * @param type The extension's class, used as the lookup key.  Must not be <jk>null</jk>.
	 * @return The extension, or {@link Optional#empty()} if none was attached under that type.
	 */
	public <X> Optional<X> extension(Class<X> type) {
		reqnn("type", type);
		return Optional.ofNullable(type.cast(extensions.get(type)));
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (! (o instanceof SearchOperator other))
			return false;
		return eq(name, other.name) && eq(help, other.help) && minArgs == other.minArgs
			&& maxArgs == other.maxArgs && eq(types, other.types) && eq(predicate, other.predicate)
			&& custom == other.custom && combinator == other.combinator && typedArgs == other.typedArgs && eq(extensions, other.extensions);
	}

	@Override /* Object */
	public int hashCode() {
		return h(name, help, minArgs, maxArgs, types, predicate, custom, combinator, typedArgs, extensions);
	}

	@Override /* Object */
	public String toString() {
		return name;
	}

	/**
	 * Builder for {@link SearchOperator}.  See the class Javadoc for an example.  Start one with
	 * {@link SearchOperator#create(String, String)} or {@link SearchOperator#copy()}.
	 */
	public static final class Builder {

		private String name;
		private String help;
		private int minArgs = 1;
		private int maxArgs = -1;
		private Set<SearchType> types = new LinkedHashSet<>();
		private SearchPredicate predicate;
		private boolean custom;
		private boolean combinator;
		private boolean typedArgs;
		private Map<Class<?>,Object> extensions = new LinkedHashMap<>();

		// Set only by copy() when the source operator was a built-in (custom == false), to the built-in's original
		// name. Left null for Builders created via create() (always custom) or the package-private builtin()
		// factory (constructs a built-in directly; it never "renames" anything). build() compares this against the
		// builder's current name only when non-null, so renaming a copy of a custom operator stays legal, and
		// builtin()'s own one-time name assignment is never flagged.
		private String builtinOriginalName;

		private Builder() {}

		/**
		 * Sets the operator name, including the leading {@code $} (e.g. {@code "$near"}).
		 *
		 * @param value The operator name.
		 * @return This object.
		 */
		public Builder name(String value) {
			name = value;
			return this;
		}

		/**
		 * Sets the popup help text.
		 *
		 * @param value The help text.
		 * @return This object.
		 */
		public Builder help(String value) {
			help = value;
			return this;
		}

		/**
		 * Sets the minimum argument count (inclusive).
		 *
		 * @param value The minimum number of arguments.
		 * @return This object.
		 */
		public Builder minArgs(int value) {
			minArgs = value;
			return this;
		}

		/**
		 * Sets the maximum argument count (inclusive), or {@code -1} for unbounded.
		 *
		 * @param value The maximum number of arguments, or {@code -1} for no upper bound.
		 * @return This object.
		 */
		public Builder maxArgs(int value) {
			maxArgs = value;
			return this;
		}

		/**
		 * Sets the value types this operator applies to.
		 *
		 * @param value The applicable types, or none to mean "applies to all types".
		 * @return This object.
		 */
		public Builder types(SearchType...value) {
			types = value == null || value.length == 0 ? new LinkedHashSet<>() : new LinkedHashSet<>(l(value));
			return this;
		}

		/**
		 * Attaches the in-memory evaluator for this (custom) operator.
		 *
		 * @param value The predicate the in-memory context calls for this operator.  Can be <jk>null</jk> to leave it
		 * 	with no in-memory evaluator.
		 * @return This object.
		 */
		public Builder predicate(SearchPredicate value) {
			predicate = value;
			return this;
		}

		/**
		 * Opts this (custom) operator's SQL renderer into typed argument values.
		 *
		 * <p>
		 * Built-in value operators ({@code $eq}/{@code $ne}/{@code $in}/{@code $gt}/{@code $gte}/{@code $lt}/
		 * {@code $lte}/{@code $between}) always receive typed arguments from {@link QueryResolver}.  For a
		 * <b>custom</b> operator, this flag controls only its SQL renderer: when <jk>true</jk>, a SQL context passes
		 * {@link SearchExpression#typedArgs()} instead of {@link SearchExpression#literalArgs()}.  The in-memory
		 * {@link SearchPredicate} always receives literal strings regardless of this flag.
		 *
		 * @param value <jk>true</jk> for the SQL renderer to receive typed arguments.
		 * @return This object.
		 */
		public Builder typedArgs(boolean value) {
			typedArgs = value;
			return this;
		}

		/**
		 * Attaches a typed extension, keyed by its own {@link Class}.
		 *
		 * <p>
		 * This is how a downstream module (for example {@code juneau-beanquery-sql}'s SQL renderer) attaches its own
		 * metadata to a custom operator without this module (commons) ever depending on that module's types (design
		 * §5.3).  Retrieve it later with {@link SearchOperator#extension(Class)}.
		 *
		 * @param <X> The extension type.
		 * @param type The extension's class, used as the lookup key.  Must not be <jk>null</jk>.
		 * @param value The extension value.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public <X> Builder extension(Class<X> type, X value) {
			reqnn("type", type);
			extensions.put(type, value);
			return this;
		}

		/**
		 * Builds the immutable {@link SearchOperator}.
		 *
		 * @return A new, immutable operator.
		 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank, {@code help} is <jk>null</jk> or
		 * 	blank, {@code minArgs} is negative, {@code maxArgs} is neither {@code -1} nor {@code >= minArgs}, or this
		 * 	builder was seeded from {@link SearchOperator#copy() copy()} of a built-in operator and {@code name} has
		 * 	been changed since.
		 */
		public SearchOperator build() {
			reqnb("name", name);
			reqnb("help", help);
			req(minArgs >= 0, "SearchOperator '%s' minArgs must not be negative: %s", name, minArgs);
			req(maxArgs == -1 || maxArgs >= minArgs,
				"SearchOperator '%s' maxArgs (%s) must be -1 (unbounded) or >= minArgs (%s).", name, maxArgs, minArgs);
			if (builtinOriginalName != null && neq(builtinOriginalName, name))
				throw iaex("Built-in operator '%s' cannot be renamed; only help text may be changed on a built-in copy.", builtinOriginalName);
			return new SearchOperator(this);
		}
	}
}
