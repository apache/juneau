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

import org.apache.juneau.commons.bean.*;

/**
 * A declarative row-decorator rule in the {@code VIEW_META} wire contract (design doc §6.3).
 *
 * <p>
 * Adds {@link #cssClass a CSS class} to a row when the rule matches, replacing a JS {@code createdRow} lambda.
 * Serializes to <c>{"field":..., "op":..., "value"?:..., "class":...}</c>.
 *
 * <p>
 * The {@link Op#EQ eq}/{@link Op#NE ne} operators <b>require</b> a {@link #value}; the
 * {@link Op#PRESENT present}/{@link Op#ABSENT absent} operators test only whether {@code row[field]} is
 * non-null/non-empty and therefore <b>omit</b> {@code value} from the wire.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="field,op,value,class")
public class RowClassRule {

	/**
	 * The comparison operator for a {@link RowClassRule}.
	 *
	 * <p>
	 * Each constant carries its lowercase wire token (the value emitted for the {@code op} field).
	 */
	public enum Op {

		/** Row matches when {@code row[field]} equals the rule value. */
		EQ("eq"),

		/** Row matches when {@code row[field]} does not equal the rule value. */
		NE("ne"),

		/** Row matches when {@code row[field]} is non-null/non-empty (no value needed). */
		PRESENT("present"),

		/** Row matches when {@code row[field]} is missing/null (no value needed). */
		ABSENT("absent");

		private final String wire;

		Op(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this operator.
		 *
		 * @return The wire token (e.g. <c>"eq"</c>).
		 */
		public String wire() {
			return wire;
		}

		/** Whether this operator requires a comparison value ({@code eq}/{@code ne}). */
		boolean requiresValue() {
			return this == EQ || this == NE;
		}
	}

	/** The row field this rule tests. */
	public String field;

	/** The wire token of the comparison operator (see {@link Op#wire()}). */
	public String op;

	/** The comparison value for {@code eq}/{@code ne}; omitted for {@code present}/{@code absent}. */
	public Object value;

	/** The CSS class to add to a matching row (serialized as the {@code class} key). */
	@BeanProp(name="class")
	public String cssClass;

	/**
	 * Creates a value-based rule for the {@link Op#EQ eq}/{@link Op#NE ne} operators.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk>.
	 * @param op The operator.  Must be {@link Op#EQ} or {@link Op#NE}.
	 * @param value The comparison value.  Must not be <jk>null</jk>.
	 * @param cssClass The CSS class to add to a matching row.  Must not be <jk>null</jk>.
	 * @return A new {@link RowClassRule}.
	 * @throws IllegalArgumentException If {@code op} is not value-based or {@code value} is <jk>null</jk>.
	 */
	public static RowClassRule of(String field, Op op, Object value, String cssClass) {
		if (! op.requiresValue())
			throw iaex("rowClassRule op '%s' does not take a value; use the (field, op, class) form.", op.wire());
		if (value == null)
			throw iaex("rowClassRule op '%s' requires a non-null value.", op.wire());
		var r = new RowClassRule();
		r.field = field;
		r.op = op.wire();
		r.value = value;
		r.cssClass = cssClass;
		return r;
	}

	/**
	 * Creates a presence-based rule for the {@link Op#PRESENT present}/{@link Op#ABSENT absent} operators.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk>.
	 * @param op The operator.  Must be {@link Op#PRESENT} or {@link Op#ABSENT}.
	 * @param cssClass The CSS class to add to a matching row.  Must not be <jk>null</jk>.
	 * @return A new {@link RowClassRule} with no {@code value}.
	 * @throws IllegalArgumentException If {@code op} is value-based.
	 */
	public static RowClassRule of(String field, Op op, String cssClass) {
		if (op.requiresValue())
			throw iaex("rowClassRule op '%s' requires a value; use the (field, op, value, class) form.", op.wire());
		var r = new RowClassRule();
		r.field = field;
		r.op = op.wire();
		r.cssClass = cssClass;
		return r;
	}
}
