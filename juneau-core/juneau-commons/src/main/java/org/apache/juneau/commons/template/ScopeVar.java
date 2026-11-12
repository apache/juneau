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
package org.apache.juneau.commons.template;

import org.apache.juneau.commons.svl.*;

/**
 * The <c>${name}</c> / <c>${name:default}</c> var of a {@link TemplateResolver}.
 *
 * <p>
 * Registered under the name <c>P</c>, which is the var the <c>${name}</c> shortcut targets, so it replaces the property
 * lookup of the default var set.  It reads the render scope of the session; nothing else is reachable through it.
 * Names are flat: a dotted <c>${a.b}</c> looks for a scope entry literally named <c>a.b</c>; use <c>#{a.b}</c> for paths.
 *
 * <p>
 * The key is read as written: {@link #allowNested()} is <jk>false</jk>, so <c>${${x}}</c> does not compute a key (which
 * would bypass catalog validation).  A {@link Structure} in <c>${x}</c> is rendered with {@link Structure#toString()}
 * and then encoded, not with the destination's renderer as <c>#{x}</c> would.
 *
 * <p>
 * The text a scope value produces is data and is never re-resolved, so {@link #allowRecurse()} is <jk>false</jk>.
 */
// Dotted ${a.b} keys are not supported: they would need a public path reader from the evaluator.
final class ScopeVar extends DefaultingVar {

	ScopeVar() {
		super("P");
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var v = session.scope().apply(key);
		if (v == null)
			return null;
		if (v instanceof String s)
			return s;
		// A Structure placed in the scope is data too: use the plain-text form so it is encoded like any value.
		return v instanceof Structure ? v.toString() : session.formatValue(v);
	}

	@Override /* Overridden from Var */
	protected boolean allowNested() {
		return false;
	}

	@Override /* Overridden from Var */
	protected boolean allowRecurse() {
		return false;
	}
}
