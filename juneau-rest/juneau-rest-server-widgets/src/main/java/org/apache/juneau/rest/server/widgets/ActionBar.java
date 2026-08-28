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
 * An ordered bar of {@link ActionRef} ids and {@link SafeAction}s.
 *
 * <p>
 * Holds ids only &mdash; it does <b>not</b> import any views-module write-action type.  CSRF, confirm, and
 * write-result handling stay on the enclosing view's action catalog.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class ActionBar implements Widget {

	/** The frozen contract version for this widget. */
	public static final String CONTRACT_VERSION = "1";

	/** The ordered items; omitted / empty means no bar. */
	public List<ActionBarItem> items;

	/**
	 * Creates an empty action bar.
	 *
	 * @return A new {@link ActionBar}.
	 */
	public static ActionBar create() {
		return new ActionBar();
	}

	/**
	 * Sets the ordered items.
	 *
	 * @param value The items, in display order.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ActionBar items(ActionBarItem...value) {
		items = l(value);
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (items == null)
			return;
		var primaryCount = 0;
		for (var item : items) {
			if (item == null)
				throw iaex("ActionBar item must not be null.");
			if (item instanceof ActionRef ar) {
				if (ar.id == null || ar.id.isBlank())
					throw iaex("ActionRef id must not be null or blank.");
				if (ar.emphasis == ActionRef.Emphasis.PRIMARY)
					primaryCount++;
				validateEnabledRules(ar);
			} else if (!(item instanceof SafeAction))
				throw iaex("ActionBar item must be ActionRef or SafeAction.");
		}
		if (primaryCount > 1)
			throw iaex("ActionBar must not have more than one PRIMARY ActionRef.");
	}

	/**
	 * Fail-closed check on an {@link ActionRef}'s row-state rules: a non-blank field, a non-blank reason, and the
	 * value/no-value shape the operator demands.
	 *
	 * <p>
	 * {@link ActionRef#enabledWhen(String,Op,Object,String)} already rejects all of this at the call, so this pass
	 * exists for a rule assembled field-by-field rather than through the fluent setter &mdash; the same reason the
	 * blank-id check above is repeated here rather than left to {@link ActionRef#of(String)}.  The reason string in
	 * particular is API contract, not a nicety: with a gated action disabled rather than removed, it is the only
	 * channel that explains an inert button, so a blank one must fail loud instead of defaulting to the action
	 * label or a generated message.
	 */
	private static void validateEnabledRules(ActionRef ar) {
		if (ar.enabledWhen == null)
			return;
		for (var r : ar.enabledWhen) {
			if (r == null)
				throw iaex("ActionRef '%s' enabledWhen rule must not be null.", ar.id);
			if (r.field == null || r.field.isBlank())
				throw iaex("ActionRef '%s' enabledWhen field must not be null or blank.", ar.id);
			if (r.op == null)
				throw iaex("ActionRef '%s' enabledWhen op must not be null.", ar.id);
			if (r.reason == null || r.reason.isBlank())
				throw iaex("ActionRef '%s' enabledWhen reason must not be null or blank.", ar.id);
			if (r.op.requiresValue() && r.value == null)
				throw iaex("ActionRef '%s' enabledWhen op '%s' requires a non-null value.", ar.id, r.op.wire());
			if (! r.op.requiresValue() && r.value != null)
				throw iaex("ActionRef '%s' enabledWhen op '%s' does not take a value.", ar.id, r.op.wire());
		}
	}
}
