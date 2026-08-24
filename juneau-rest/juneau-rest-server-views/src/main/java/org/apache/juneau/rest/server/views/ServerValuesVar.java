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

import org.apache.juneau.commons.svl.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The <js>"$FV{name}"</js> / <js>"$FV{name,default}"</js> server-values variable.
 *
 * <p>
 * Resolves author-declared scalar values from a {@link ServerValuesRegistry} placed in the per-render session by
 * the widget emitter.  It is a {@link DefaultingVar}: <js>"$FV{name}"</js> resolves the named provider and
 * <js>"$FV{name,default}"</js> falls back to {@code default} when the provider is missing or returns
 * <jk>null</jk>.
 *
 * <p>
 * <b>Not {@code $W}.</b>  This var emits declared scalar values as plain text (the caller places the returned
 * string into html5 beans and the serializer entity-encodes it).  It is distinct from {@code $W}
 * ({@code org.apache.juneau.html.HtmlWidgetVar}), which emits <b>raw</b> widget HTML.  {@code $FV} never injects
 * HTML.
 *
 * <p>
 * {@link #allowRecurse()} is <jk>false</jk>: a provider's returned string is emitted literally and is never
 * re-parsed as SVL (untrusted computed data must not be recursively resolved).  When no registry is present in
 * the session the var is not resolvable and the <js>"$FV{...}"</js> token is left literal (fail-open).
 *
 * @since 10.0.0
 */
public class ServerValuesVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "FV";

	/**
	 * Constructor.
	 */
	public ServerValuesVar() {
		super(NAME);
	}

	@Override /* Overridden from Var */
	protected boolean allowRecurse() {
		return false;
	}

	@Override /* Overridden from Var */
	protected boolean canResolve(VarResolverSession session) {
		return session.getBean(ServerValuesRegistry.class).isPresent();
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var reg = session.getBean(ServerValuesRegistry.class).orElse(null);
		if (reg == null)
			return null;
		return reg.resolve(session, key);
	}
}
