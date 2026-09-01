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
import org.apache.juneau.rest.server.widgets.Op;

/**
 * A declarative disable-with-reason rule gating a {@link RowAction} on the row's own state, in the
 * {@code VIEW_META} wire contract.
 *
 * <p>
 * Mirrors {@code org.apache.juneau.rest.server.widgets.ActionRef.EnabledRule} on the detail-panel
 * {@code ActionBar} (widgets module), but rides the wire as a plain data rule the same way {@link RowClassRule}
 * does &mdash; a table row is polled/client-rendered rather than server-painted, so this type follows
 * {@link RowClassRule}'s shape, not {@code ActionRef.EnabledRule}'s Java-only one.  Serializes to
 * <c>{"field":..., "op":..., "value"?:..., "reason":...}</c>, with {@link #op} carrying {@link Op#wire()}'s
 * lowercase token (a {@link String}), not the Java enum.
 *
 * <p>
 * The {@link Op#EQ eq}/{@link Op#NE ne} operators <b>require</b> a {@link #value}; the {@link Op#PRESENT
 * present}/{@link Op#ABSENT absent} operators test only whether {@code row[field]} is non-null/non-empty and
 * therefore <b>omit</b> {@code value} from the wire.
 *
 * <h5 class='section'>Presentation only, never the authorization gate:</h5>
 * <p>
 * A row whose field does not satisfy this rule renders its {@link RowAction} present but disabled, carrying
 * {@link #reason} as a tooltip and {@code aria-describedby} text.  The endpoint behind the action must still
 * perform its own real check and refuse on its own authority; this rule is never a substitute for that check.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link RowAction}
 * 	<li class='jc'>{@link RowClassRule}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="field,op,value,reason")
public class RowActionEnabledRule {

	/** The row field this rule tests. */
	public String field;

	/** The wire token of the comparison operator (see {@link Op#wire()}). */
	public String op;

	/** The comparison value for {@code eq}/{@code ne}; omitted for {@code present}/{@code absent}. */
	public Object value;

	/** Why the action is unavailable when this rule does not match.  Required &mdash; must not be blank. */
	public String reason;

	/**
	 * Creates a value-based rule for the {@link Op#EQ eq}/{@link Op#NE ne} operators.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk> or blank.
	 * @param op The operator.  Must be {@link Op#EQ} or {@link Op#NE}.
	 * @param value The comparison value.  Must not be <jk>null</jk>.
	 * @param reason Why the action is unavailable when this rule does not match.  Must not be <jk>null</jk> or
	 * 	blank.
	 * @return A new {@link RowActionEnabledRule}.
	 * @throws IllegalArgumentException If {@code field}/{@code reason} is <jk>null</jk> or blank, {@code op} is
	 * 	not value-based, or {@code value} is <jk>null</jk>.
	 */
	public static RowActionEnabledRule of(String field, Op op, Object value, String reason) {
		if (field == null || field.isBlank())
			throw iaex("RowActionEnabledRule field must not be null or blank.");
		if (op == null || ! op.requiresValue())
			throw iaex("RowActionEnabledRule op '%s' does not take a value; use the (field, op, reason) form.",
				op == null ? "null" : op.wire());
		if (value == null)
			throw iaex("RowActionEnabledRule op '%s' requires a non-null value.", op.wire());
		if (reason == null || reason.isBlank())
			throw iaex("RowActionEnabledRule reason must not be null or blank.");
		var r = new RowActionEnabledRule();
		r.field = field;
		r.op = op.wire();
		r.value = value;
		r.reason = reason;
		return r;
	}

	/**
	 * Creates a presence-based rule for the {@link Op#PRESENT present}/{@link Op#ABSENT absent} operators.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk> or blank.
	 * @param op The operator.  Must be {@link Op#PRESENT} or {@link Op#ABSENT}.
	 * @param reason Why the action is unavailable when this rule does not match.  Must not be <jk>null</jk> or
	 * 	blank.
	 * @return A new {@link RowActionEnabledRule} with no {@code value}.
	 * @throws IllegalArgumentException If {@code field}/{@code reason} is <jk>null</jk> or blank, or {@code op}
	 * 	is value-based.
	 */
	public static RowActionEnabledRule of(String field, Op op, String reason) {
		if (field == null || field.isBlank())
			throw iaex("RowActionEnabledRule field must not be null or blank.");
		if (op == null || op.requiresValue())
			throw iaex("RowActionEnabledRule op '%s' requires a value; use the (field, op, value, reason) form.",
				op == null ? "null" : op.wire());
		if (reason == null || reason.isBlank())
			throw iaex("RowActionEnabledRule reason must not be null or blank.");
		var r = new RowActionEnabledRule();
		r.field = field;
		r.op = op.wire();
		r.reason = reason;
		return r;
	}
}
