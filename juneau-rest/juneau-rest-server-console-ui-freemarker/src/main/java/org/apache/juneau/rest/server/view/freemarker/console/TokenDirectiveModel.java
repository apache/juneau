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
 * two disjoint {@link org.apache.juneau.rest.server.console.ThemePack} channels:
 * <ul class='spaced-list'>
 * 	<li>{@code value=} is a <b>leaf</b>: a CSS literal, or a {@code var(--jc-name)} reference that
 * 		{@link org.apache.juneau.rest.server.console.Theme.Builder#token(String, String)} <b>resolves to a literal</b>
 * 		at pack build (the substring {@code var(} does not reach the served leaf map).
 * 	<li>{@code alias=} is a <b>derived reference</b>: a {@code var(--jc-name)} that
 * 		{@link org.apache.juneau.rest.server.console.ThemePack.Builder#alias(String, String)} keeps as a reference
 * 		<b>surviving to the wire</b>. A literal cannot express a live alias &mdash; that is why the attr exists.
 * </ul>
 *
 * <p>
 * The name/value/target guards and reserved-namespace rejection are reused verbatim from the Java
 * {@code Theme}/{@code ThemePack} builders (fail closed); an {@link IllegalArgumentException} they raise is
 * re-surfaced through {@link FtlAttrLists#reject(String)} so its exact sentence reaches the HTTP 500 body. The
 * directive writes no markup &mdash; it folds the declaration into the enclosing {@link ThemeBuildContext}, which
 * {@code <@theme>} assembles into a pack after its body pass.
 *
 * @since 10.0.0
 */
public final class TokenDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "token";

	private static final Set<String> ATTRS = Set.of("name", "value", "alias");

	TokenDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked", // FreeMarker's raw params Map is String-keyed by contract.
		"deprecation" // Theme.Builder.token is this directive's own leaf channel; the deprecation steers CONSUMER Java authoring to <@token>, which IS this directive.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		var p = (Map<String, TemplateModel>) params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@token> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var ctx = (ThemeBuildContext) env.getCustomState(ThemeBuildContext.KEY);
		if (ctx == null)
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
				ctx.packBuilder.alias(name, FtlAttrLists.scalar(p, "alias"));
		} catch (IllegalArgumentException e) {
			// The Java builders' frozen sentence is the diagnostic; re-surface it undecorated (see FtlAttrLists.reject).
			throw FtlAttrLists.reject(e.getMessage());
		}
		ctx.anyDeclared = true;
	}
}
