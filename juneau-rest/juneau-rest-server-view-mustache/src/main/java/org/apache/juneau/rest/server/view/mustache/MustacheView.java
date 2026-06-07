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
package org.apache.juneau.rest.server.view.mustache;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.rest.server.view.*;

/**
 * Immutable value class returned from {@code @RestOp}-annotated methods to ask the framework to
 * render a Mustache template.
 *
 * <p>
 * Companion to {@link MustacheMixin} and {@link MustacheViewRenderer}: the mixin sets up
 * the {@code /mustache/*} mount and registers the renderer; the renderer detects
 * {@code MustacheView} returns in the response-processor chain and asks the configured
 * {@code com.github.mustachejava.MustacheFactory} to {@code compile(templateName)} and then
 * {@code execute(writer, scope)} directly onto the response writer.
 *
 * <h5 class='figure'>Usage:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 	<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 		<jk>return</jk> MustacheView.<jsm>of</jsm>(<js>"hello.mustache"</js>)
 * 			.attr(<js>"name"</js>, <jv>name</jv>)
 * 			.attr(<js>"ts"</js>, Instant.<jsm>now</jsm>());
 * 	}
 * </p>
 *
 * <p>
 * The template name is factory-relative &mdash; the configured
 * {@code com.github.mustachejava.MustacheFactory} resource resolver prepends its own resource
 * root. With the bridge's default factory + {@code basePath("/templates/")},
 * {@code MustacheView.of("hello.mustache")} resolves to {@code templates/hello.mustache} on the
 * classpath.
 *
 * <h5 class='section'>Template suffix:</h5>
 *
 * <p>
 * Unlike the Thymeleaf bridge (which always appends {@code .html} via the engine resolver's
 * suffix setting), mustache.java does not have a built-in resolver-suffix concept &mdash; the
 * literal template name is what {@code factory.compile(...)} sees. The
 * {@link MustacheMixin.Builder#templateSuffix(String) templateSuffix(...)} builder knob
 * fills the gap: when set, the bridge appends the suffix to template names that don't already
 * end with it (idempotent), so the caller can write {@code MustacheView.of("hello")} and the
 * bridge resolves it as {@code "hello.mustache"} on the classpath.
 *
 * <h5 class='section'>Immutability:</h5>
 *
 * <p>
 * Each {@code attr(...)} / {@code attrs(...)} / {@code header(...)} call returns a <b>new</b>
 * {@code MustacheView} carrying the additional binding; the original instance is unchanged. This
 * keeps {@code MustacheView} a value type safe to share across requests (e.g. as a static
 * singleton built once and returned from many handlers).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MustacheMixin}
 * 	<li class='jc'>{@link MustacheViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class MustacheView implements View {

	private final String templateName;
	private final Map<String, Object> attributes;
	private final Map<String, String> responseHeaders;

	/**
	 * Creates a new {@code MustacheView} carrying the given template name and no attributes.
	 *
	 * <p>
	 * The template name is factory-relative; the configured
	 * {@code com.github.mustachejava.MustacheFactory} resource resolver supplies the prefix
	 * (resource root). With the bridge's default factory + {@code basePath("/templates/")}
	 * and {@code templateSuffix(".mustache")}, {@code "hello"} resolves to
	 * {@code templates/hello.mustache}; without {@code templateSuffix}, the literal name is
	 * used as-is.
	 *
	 * @param templateName The template name (factory-relative, suffix optional). Must not be
	 * 	{@code null} or blank.
	 * @return A new {@code MustacheView} instance.
	 * @throws IllegalArgumentException If {@code templateName} is {@code null} or blank.
	 */
	public static MustacheView of(String templateName) {
		if (isBlank(templateName))
			throw illegalArg("templateName must not be null or blank");
		return new MustacheView(templateName, Map.of(), Map.of());
	}

	private MustacheView(String templateName, Map<String, Object> attributes, Map<String, String> responseHeaders) {
		this.templateName = templateName;
		this.attributes = attributes;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * Returns a copy of this view with the given attribute added (or replaced if a binding for
	 * {@code key} already exists).
	 *
	 * <p>
	 * The full attribute map is passed to mustache.java as the rendering scope, so each entry
	 * is accessible inside the template as {@code {{key}}}. {@code null} values are rejected at
	 * build-time because a {@code null} scope binding in mustache.java renders as an empty
	 * string, which silently masks what is typically a caller bug; rejecting up-front surfaces
	 * the mistake at the call site (matches the Thymeleaf-bridge precedent).
	 *
	 * @param key The attribute key (exposed inside the template as {@code {{key}}}). Must not be
	 * 	{@code null} or blank.
	 * @param value The attribute value. Must not be {@code null}.
	 * @return A new {@code MustacheView} carrying the additional attribute.
	 * @throws IllegalArgumentException If {@code key} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public MustacheView attr(String key, Object value) {
		if (isBlank(key))
			throw illegalArg("attribute key must not be null or blank");
		if (value == null)
			throw illegalArg("attribute value must not be null (attribute ''{0}'')", key);
		var copy = new LinkedHashMap<>(attributes);
		copy.put(key, value);
		return new MustacheView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with all entries from the given map added (or replaced if a
	 * binding for any key already exists).
	 *
	 * @param values The attributes to add. {@code null} is treated as an empty map (the result is
	 * 	the same instance with no entries added).
	 * @return A new {@code MustacheView} carrying the additional attributes.
	 * @throws IllegalArgumentException If any entry has a {@code null}/blank key or a {@code null}
	 * 	value.
	 */
	public MustacheView attrs(Map<String, ?> values) {
		if (values == null || values.isEmpty())
			return this;
		var copy = new LinkedHashMap<>(attributes);
		values.forEach((k, v) -> {
			if (isBlank(k))
				throw illegalArg("attribute key must not be null or blank");
			if (v == null)
				throw illegalArg("attribute value must not be null (attribute ''{0}'')", k);
			copy.put(k, v);
		});
		return new MustacheView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with the given response header set on the rendered response.
	 *
	 * <p>
	 * Typical usage: {@code .header("Content-Type", "text/html; charset=UTF-8")} or
	 * {@code .header("Cache-Control", "no-store")}. The renderer applies these headers via
	 * {@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
	 * HttpServletResponse.setHeader(...)} before writing the rendered output.
	 *
	 * @param name The header name. Must not be {@code null} or blank.
	 * @param value The header value. Must not be {@code null}.
	 * @return A new {@code MustacheView} carrying the additional response header.
	 * @throws IllegalArgumentException If {@code name} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public MustacheView header(String name, String value) {
		if (isBlank(name))
			throw illegalArg("header name must not be null or blank");
		if (value == null)
			throw illegalArg("header value must not be null (header ''{0}'')", name);
		var copy = new LinkedHashMap<>(responseHeaders);
		copy.put(name, value);
		return new MustacheView(templateName, attributes, Map.copyOf(copy));
	}

	@Override /* Overridden from View */
	public String getTemplateName() {
		return templateName;
	}

	@Override /* Overridden from View */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override /* Overridden from View */
	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "MustacheView[template=" + templateName + ", attrs=" + attributes.keySet()
			+ ", headers=" + responseHeaders.keySet() + "]";
	}
}
