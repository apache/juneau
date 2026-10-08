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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@token name="--jc-…" value="…"/>} / {@code <@token name="--jc-…" alias="var(--jc-…)"/>} chrome
 * FreeMarker directive: one custom theme-token override authored inside a {@code <@theme>} element.
 *
 * <p>
 * Exactly one of {@code value=} / {@code alias=} is required, and they are mutually exclusive &mdash; they are the
 * two disjoint {@link org.apache.juneau.rest.server.console.Theme} channels:
 * <ul class='spaced-list'>
 * 	<li>{@code value=} is a <b>leaf</b>: a CSS literal, or a {@code var(--jc-name)} reference that
 * 		{@link org.apache.juneau.rest.server.console.Theme.Builder#token(String, String)} <b>resolves to a literal</b>
 * 		at build time (the substring {@code var(} does not reach the served leaf map).
 * 	<li>{@code alias=} is a <b>derived reference</b>: a {@code var(--jc-name)} that
 * 		{@link org.apache.juneau.rest.server.console.Theme.Builder#alias(String, String)} keeps as a reference
 * 		<b>surviving to the wire</b>. A literal cannot express a live alias &mdash; that is why the attr exists.
 * </ul>
 *
 * <p>
 * The name/value/target guards and reserved-namespace rejection are the Java {@code Theme.Builder}'s own (fail
 * closed); an {@link IllegalArgumentException} it raises is re-surfaced through {@link FtlAttrLists#reject(String)}
 * so its exact sentence reaches the HTTP 500 body. The directive writes no markup &mdash; it folds the declaration
 * into the enclosing {@link ThemeBuildContext}, which {@code <@theme>} builds after its body pass.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bftl'>
 * 	&lt;@theme name="light-red"&gt;
 * 		&lt;#-- A leaf: a literal. --&gt;
 * 		&lt;@token name="--jc-pill-red-bg" value="#fdeceb"/&gt;
 * 		&lt;#-- An alias: a live reference. --&gt;
 * 		&lt;@token name="--jc-tab-bar-bg" alias="var(--jc-card-bg)"/&gt;
 * 	&lt;/@theme&gt;
 *
 * 	&lt;#-- Rejected: both value= and alias=, neither one, or a reserved --jc-chrome-* name. --&gt;
 * 	&lt;@token name="--jc-x" value="#fff" alias="var(--jc-accent)"/&gt;
 * 	&lt;@token name="--jc-x"/&gt;
 * 	&lt;@token name="--jc-chrome-control-height" value="26px"/&gt;
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
public final class TokenDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "token";

	static final Set<String> ATTRS = Set.of("name", "value", "alias");

	TokenDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@token> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var cap = PageCapture.get(env);
		var ctx = n(cap) ? null : cap.themeBuild;
		if (n(ctx))
			throw FtlAttrLists.reject("<@token> must be nested inside <@theme>.");

		var name = FtlAttrLists.scalar(p, "name");
		if (name.isEmpty())
			throw FtlAttrLists.reject("<@token> requires name=.");

		var hasValue = p.containsKey("value");
		var hasAlias = p.containsKey("alias");
		if (hasValue == hasAlias)
			throw FtlAttrLists.reject("<@token name=\"" + name + "\"> requires exactly one of value= or alias=.");

		try {
			if (hasValue)
				ctx.themeBuilder.token(name, FtlAttrLists.scalar(p, "value"));
			else
				ctx.themeBuilder.alias(name, FtlAttrLists.scalar(p, "alias"));
		} catch (IllegalArgumentException e) {
			// Theme.Builder's frozen sentence is the diagnostic; re-surface it undecorated (see FtlAttrLists.reject).
			throw FtlAttrLists.reject(e.getMessage());
		}
		ctx.anyDeclared = true;
	}
}
