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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * An opaque id naming a write action on the enclosing view.
 *
 * <p>
 * This type deliberately does <b>not</b> import any views-module type.  Existence of the named action on the
 * enclosing view's action catalog is checked in views ({@code RowDetailDef}/{@code ViewDef.validate()}), never
 * here.  The bar does not grow its own write protocol (no endpoint / method / csrf fields).
 *
 * @since 10.0.0
 */
public class ActionRef implements ActionBarItem {

	/** The opaque action id.  Must not be blank. */
	public String id;

	/** The visual weight this bar paints for this action.  Defaults to {@link Emphasis#SECONDARY}. */
	public Emphasis emphasis = Emphasis.SECONDARY;

	/**
	 * The row-state rules that must all match for this action to be offered, in the author's declared order.
	 *
	 * <p>
	 * {@code null} or empty means the action is never gated on row state.  See
	 * {@link #enabledWhen(String,Op,Object,String)} for the semantics, including the deliberate absence of a
	 * hide/show mode.
	 */
	public List<EnabledRule> enabledWhen;

	/**
	 * Creates an action reference with the specified id.
	 *
	 * @param id The action id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link ActionRef}.
	 */
	public static ActionRef of(String id) {
		if (id == null || id.isBlank())
			throw iaex("ActionRef id must not be null or blank.");
		var a = new ActionRef();
		a.id = id;
		return a;
	}

	/**
	 * Sets the visual weight this bar paints for this action.
	 *
	 * <p>
	 * This is a per-bar rendering property, not a property of the action itself &mdash; the same action can be
	 * the primary button in one bar (e.g. the Detail View header) and an ordinary item in another.  Java-only:
	 * never marshalled into {@code VIEW_META}, so it does not move {@link ActionBar#CONTRACT_VERSION}.
	 *
	 * @param value The emphasis.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ActionRef emphasis(Emphasis value) {
		this.emphasis = value;
		return this;
	}

	/**
	 * Gates this action on the row's own state, comparing one field against one value.
	 *
	 * <p>
	 * A row whose field does not satisfy every declared rule is offered the action <b>present but disabled</b>,
	 * carrying {@code reason} as its explanation.  Call this more than once to declare more than one rule; all of
	 * them must match.
	 *
	 * <h5 class='section'>This is presentation only &mdash; never the authorization gate:</h5>
	 * <p>
	 * The rule exists so an operator is not offered a button that cannot possibly work.  The server still re-reads
	 * the row's state and refuses on its own authority; a client-side rule that quietly became the only check would
	 * be a regression, not an optimization.  Nothing here is a substitute for the server's own refusal.
	 *
	 * <h5 class='section'>Disabled, never hidden:</h5>
	 * <p>
	 * There is deliberately no {@code visibleWhen(...)} and no hidden mode.  Removing a momentarily-impossible
	 * action makes the bar's contents jump between rows and hides the fact that the action exists at all.  When an
	 * action is invalid for an <b>entire</b> view rather than per row, declare an {@link ActionBar} for that view
	 * that simply omits it &mdash; that is the right tool, and it is why per-row hiding is not offered.
	 *
	 * <h5 class='section'>Which reason an operator sees:</h5>
	 * <p>
	 * When several rules on one action fail, the <b>first-declared</b> one wins and evaluation stops there.  The
	 * author's ordering is therefore the priority mechanism: no severity field, no concatenation, no
	 * most-specific-rule heuristic.  The runtime surfaces that one reason through both a native tooltip and
	 * {@code aria-describedby}, and clears both again when the rules pass.
	 *
	 * <h5 class='section'>A field the row does not carry fails closed:</h5>
	 * <p>
	 * A rule keyed on a field the enclosing panel never declares is rejected at startup.  If the field is
	 * nonetheless missing from a row's payload at runtime, the rule <b>fails</b> and the action disables &mdash;
	 * including for {@link Op#ABSENT}, which tests a field that is present and empty rather than one that was
	 * never returned.
	 *
	 * <p>
	 * Java-only, like {@link #emphasis}: the bar is painted into server-emitted markup and is never marshalled into
	 * the view's JSON sidecar, so this does not move {@link ActionBar#CONTRACT_VERSION}.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk> or blank.
	 * @param op The comparison operator.  Must be {@link Op#EQ} or {@link Op#NE}.
	 * @param value The comparison value.  Must not be <jk>null</jk>.
	 * @param reason Why the action is unavailable when this rule does not match.  Must not be <jk>null</jk> or
	 * 	blank &mdash; with the action disabled rather than removed, this string is the only thing that explains an
	 * 	inert button.
	 * @return This object.
	 */
	public ActionRef enabledWhen(String field, Op op, Object value, String reason) {
		if (op == null || ! op.requiresValue())
			throw iaex("enabledWhen op '%s' does not take a value; use the (field, op, reason) form.",
				op == null ? "null" : op.wire());
		if (value == null)
			throw iaex("enabledWhen op '%s' requires a non-null value.", op.wire());
		return addEnabledRule(field, op, value, reason);
	}

	/**
	 * Gates this action on the presence or absence of a row field.
	 *
	 * <p>
	 * The presence-based counterpart of {@link #enabledWhen(String,Op,Object,String)}, which carries the full
	 * semantics &mdash; presentation only, disabled rather than hidden, first-declared reason wins, and a field the
	 * row does not carry fails closed.
	 *
	 * @param field The row field to test.  Must not be <jk>null</jk> or blank.
	 * @param op The comparison operator.  Must be {@link Op#PRESENT} or {@link Op#ABSENT}.
	 * @param reason Why the action is unavailable when this rule does not match.  Must not be <jk>null</jk> or
	 * 	blank.
	 * @return This object.
	 */
	public ActionRef enabledWhen(String field, Op op, String reason) {
		if (op == null || op.requiresValue())
			throw iaex("enabledWhen op '%s' requires a value; use the (field, op, value, reason) form.",
				op == null ? "null" : op.wire());
		return addEnabledRule(field, op, null, reason);
	}

	private ActionRef addEnabledRule(String field, Op op, Object value, String reason) {
		if (field == null || field.isBlank())
			throw iaex("enabledWhen field must not be null or blank.");
		if (reason == null || reason.isBlank())
			throw iaex("enabledWhen reason must not be null or blank.");
		var r = new EnabledRule();
		r.field = field;
		r.op = op;
		r.value = value;
		r.reason = reason;
		if (enabledWhen == null)
			enabledWhen = new ArrayList<>();
		enabledWhen.add(r);
		return this;
	}

	/** The visual weight an {@link ActionRef} button is painted with by the {@link ActionBar} that renders it. */
	public enum Emphasis {

		/** The bar's single most prominent action &mdash; solid accent fill.  At most one per bar. */
		PRIMARY,

		/** The default weight &mdash; neutral fill, accent-coloured label. */
		SECONDARY
	}

	/**
	 * One row-state rule gating an {@link ActionRef}.
	 *
	 * <p>
	 * Built by {@link ActionRef#enabledWhen(String,Op,Object,String)}, which is the only shape that can produce a
	 * well-formed one; a hand-assembled instance is checked again by {@link ActionBar#validate()}, so neither route
	 * can smuggle a rule with no {@link #reason} past the bar.
	 */
	public static class EnabledRule {

		/** The row field this rule tests.  Must not be blank. */
		public String field;

		/** The comparison operator.  Must not be <jk>null</jk>. */
		public Op op;

		/**
		 * The comparison value for {@link Op#EQ}/{@link Op#NE}; must be <jk>null</jk> for
		 * {@link Op#PRESENT}/{@link Op#ABSENT}.
		 */
		public Object value;

		/** Why the action is unavailable when this rule does not match.  Required &mdash; must not be blank. */
		public String reason;
	}
}
