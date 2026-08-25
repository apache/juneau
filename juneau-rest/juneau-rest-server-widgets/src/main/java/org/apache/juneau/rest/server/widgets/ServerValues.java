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
import java.util.function.*;

import org.apache.juneau.commons.svl.*;

/**
 * A {@link org.apache.juneau.rest.server.widgets.Widget}-marked declaration of named server-side scalar values.
 *
 * <p>
 * This is a thin, typed facade over the existing {@link VarResolver} engine &mdash; <b>not</b> a second
 * templating language.  An author declares named scalar value providers (for example {@code failedCount} or
 * {@code lookbackReadable}); a widget emitter publishes them into a per-render registry and interpolates their
 * resolved values into widget <i>chrome</i> text (titles / labels) as <js>"$FV{name}"</js> /
 * <js>"$FV{name,default}"</js> at serve time.
 *
 * <p>
 * <b>Not {@code $W}.</b>  {@code $FV} resolves declared scalar values as plain text (serializer-encoded); it is
 * distinct from {@code $W} ({@code HtmlWidgetVar}), which emits raw widget HTML.  {@code $FV} never injects HTML.
 *
 * <p>
 * Providers must return a scalar ({@link String} / {@link Number} / {@link Boolean}) or <jk>null</jk>; non-scalar
 * results are rejected at resolve time (no {@code List.toString()} join &mdash; collection rendering is out of
 * scope here).  Values are session-aware: each provider is a {@link Function} of the per-render
 * {@link VarResolverSession}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ServerValues <jv>sv</jv> = ServerValues.<jsm>create</jsm>()
 * 		.value(<js>"failedCount"</js>, <jv>session</jv> -&gt; <jv>metrics</jv>.failed())
 * 		.value(<js>"env"</js>, <jv>session</jv> -&gt; <js>"prod"</js>);
 * 	<jc>// Chrome: column title "Failures ($FV{failedCount})" resolves at serve time.</jc>
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class ServerValues implements Widget {

	/** The frozen contract version for this widget. */
	public static final String CONTRACT_VERSION = "1";

	/**
	 * Ordered map of interpolation name &rarr; value declaration.
	 *
	 * <p>
	 * A {@link LinkedHashMap} for stable {@link #validate()} / emit order.  This field is <b>Java-only</b> &mdash;
	 * it is never placed on a {@code @BeanType} and never marshals to VIEW_META (lambda providers are not a wire
	 * type).
	 */
	public Map<String,ServerValuesValue> values = new LinkedHashMap<>();

	/**
	 * Creates an empty declaration.
	 *
	 * @return A new {@link ServerValues}.
	 */
	public static ServerValues create() {
		return new ServerValues();
	}

	/**
	 * Declares a named scalar value provider.
	 *
	 * @param name The interpolation name (the {@code x} in <js>"$FV{x}"</js>).  Must not be <jk>null</jk> or blank.
	 * @param provider The session-aware scalar provider.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ServerValues value(String name, Function<VarResolverSession,?> provider) {
		if (name == null || name.isBlank())
			throw iaex("ServerValues name must not be null or blank.");
		if (provider == null)
			throw iaex("ServerValues provider for ''{0}'' must not be null.", name);
		if (values.containsKey(name))
			throw iaex("ServerValues duplicate name ''{0}''.", name);
		values.put(name, ServerValuesValue.of(name, provider));
		return this;
	}

	/**
	 * Sets the full ordered map of value declarations.
	 *
	 * @param value The declarations, keyed by interpolation name.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public ServerValues values(Map<String,ServerValuesValue> value) {
		values = value;
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (values == null)
			return;
		for (var e : values.entrySet()) {
			var name = e.getKey();
			if (name == null || name.isBlank())
				throw iaex("ServerValues name must not be null or blank.");
			var v = e.getValue();
			if (v == null)
				throw iaex("ServerValues value for ''{0}'' must not be null.", name);
			if (v.provider == null)
				throw iaex("ServerValues provider for ''{0}'' must not be null.", name);
		}
	}
}
