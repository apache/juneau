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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.rest.server.widgets.Op;

/**
 * A presentation-only visibility rule: the element is shown only when every rule in its list matches.
 *
 * <p>
 * Rules hide UI; they never authorize.  The endpoint behind a hidden action must still check the caller.
 * Use {@code visible(false)} instead when the element must not reach the client at all.
 *
 * <p>
 * The evaluation map depends on the element's level.  A row action, detail field or region sees the row plus the
 * page's {@code facts} under a {@code facts.} prefix; a card, badge or ribbon item sees the page's top-level
 * {@code facts} alone.  A field absent from the map fails closed for every operator except {@code absent}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RowAction.<jsm>create</jsm>(<js>"escalate"</js>)
 * 		.visibleWhen(
 * 			VisibilityRule.<jsm>when</jsm>(<js>"status"</js>).in(<js>"open"</js>, <js>"acked"</js>),
 * 			VisibilityRule.<jsm>when</jsm>(<js>"facts.viewer.roles"</js>).contains(<js>"oncall"</js>));
 * </p>
 *
 * @since 10.0.0
 */
@BeanType(properties="field,op,value")
public final class VisibilityRule {

	private static final Object MISSING = new Object();
	static final Set<String> KNOWN_OPS = Set.of("eq", "ne", "present", "absent", "in", "contains");

	/** The field this rule tests, a dotted path into the evaluation map (e.g. {@code "facts.viewer.roles"}). */
	public String field;

	/** The wire token of the comparison operator (see {@link Op#wire()}). */
	public String op;

	/** The comparison value for {@code eq}/{@code ne}/{@code in}/{@code contains}; omitted for {@code present}/{@code absent}. */
	public Object value;

	private VisibilityRule() {}

	/**
	 * Starts a rule for the given field.
	 *
	 * @param field The dotted-path field to test.  Must not be <jk>null</jk> or blank.
	 * @return A {@link Builder} to pick the operator.
	 * @throws IllegalArgumentException If {@code field} is <jk>null</jk> or blank.
	 */
	public static Builder when(String field) {
		if (field == null || field.isBlank())
			throw iaex("VisibilityRule field must not be null or blank.");
		return new Builder(field);
	}

	/**
	 * Evaluates a list of rules against an evaluation map; every rule must match (AND).
	 *
	 * <p>
	 * A <jk>null</jk> or empty list always matches.  A field absent from {@code map} never satisfies
	 * {@code eq}/{@code ne}/{@code in}/{@code contains}/{@code present}; only {@code absent} treats a missing field
	 * as a match.  Mirrors the JS {@code JuneauViews.rules.test} evaluator exactly.
	 *
	 * @param rules The rules to test, or <jk>null</jk>.
	 * @param map The evaluation map.
	 * @return <jk>true</jk> when every rule matches.
	 * @throws IllegalArgumentException If a rule's {@code op} is not one of the known tokens.
	 */
	public static boolean test(List<VisibilityRule> rules, Map<String,Object> map) {
		if (rules == null || rules.isEmpty())
			return true;
		for (var r : rules)
			if (! r.matches(map))
				return false;
		return true;
	}

	/**
	 * Serializes this rule to its wire shape.
	 *
	 * @return An ordered {@link Map} of {@code field}, {@code op} and (when set) {@code value}.
	 */
	public Map<String,Object> toMap() {
		var m = new LinkedHashMap<String,Object>();
		m.put("field", field);
		m.put("op", op);
		if (value != null)
			m.put("value", value);
		return m;
	}

	static List<Map<String,Object>> toMaps(List<VisibilityRule> rules) {
		return rules.stream().map(VisibilityRule::toMap).toList();
	}

	private boolean matches(Map<String,Object> map) {
		if (! KNOWN_OPS.contains(op))
			throw iaex("VisibilityRule op '%s' is not one of '%s'.", op, String.join(", ", new TreeSet<>(KNOWN_OPS)));
		var actual = resolve(map, field);
		var missing = actual == MISSING;
		return switch (op) {
			case "eq" -> ! missing && Objects.equals(actual, value);
			case "ne" -> ! missing && ! Objects.equals(actual, value);
			case "present" -> ! missing && actual != null && ! (actual instanceof String s && s.isBlank());
			case "absent" -> missing || actual == null || (actual instanceof String s && s.isBlank());
			case "in" -> ! missing && value instanceof Collection<?> c && c.contains(actual);
			default -> ! missing && actual instanceof Collection<?> c && c.contains(value);
		};
	}

	private static Object resolve(Map<?,?> map, String dottedPath) {
		Object cur = map;
		for (var part : dottedPath.split("\\."))
			if (cur instanceof Map<?,?> m && m.containsKey(part))
				cur = m.get(part);
			else
				return MISSING;
		return cur;
	}

	/** Fluent operator picker returned by {@link VisibilityRule#when(String)}. */
	public static final class Builder {
		private final String field;

		private Builder(String field) {
			this.field = field;
		}

		/**
		 * Matches when the field equals the value.
		 *
		 * @param value The comparison value.  Must not be <jk>null</jk>.
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule eq(Object value) { return of(Op.EQ, value); }

		/**
		 * Matches when the field is present and does not equal the value.
		 *
		 * @param value The comparison value.  Must not be <jk>null</jk>.
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule ne(Object value) { return of(Op.NE, value); }

		/**
		 * Matches when the field is non-null and non-blank.
		 *
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule present() { return of(Op.PRESENT, null); }

		/**
		 * Matches when the field is missing, null or blank.
		 *
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule absent() { return of(Op.ABSENT, null); }

		/**
		 * Matches when the field equals one of the values.
		 *
		 * @param values The candidate values.
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule in(Object...values) { return of(Op.IN, List.of(values)); }

		/**
		 * Matches when the field (itself a list) contains the value.
		 *
		 * @param value The value the list must contain.  Must not be <jk>null</jk>.
		 * @return A new {@link VisibilityRule}.
		 */
		public VisibilityRule contains(Object value) { return of(Op.CONTAINS, value); }

		private VisibilityRule of(Op op, Object value) {
			if (op.requiresValue() && value == null)
				throw iaex("VisibilityRule op '%s' requires a non-null value.", op.wire());
			var r = new VisibilityRule();
			r.field = field;
			r.op = op.wire();
			r.value = value;
			return r;
		}
	}
}
