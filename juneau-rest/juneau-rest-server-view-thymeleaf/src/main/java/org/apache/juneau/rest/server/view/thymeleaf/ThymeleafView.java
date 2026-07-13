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
package org.apache.juneau.rest.server.view.thymeleaf;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.rest.server.view.*;

/**
 * Immutable value class returned from {@code @RestOp}-annotated methods to ask the framework to
 * render a Thymeleaf template.
 *
 * <p>
 * Companion to {@link ThymeleafMixin} and {@link ThymeleafViewRenderer}: the mixin sets up
 * the {@code /thymeleaf/*} mount and registers the renderer; the renderer detects
 * {@code ThymeleafView} returns in the response-processor chain and asks the configured
 * {@code org.thymeleaf.TemplateEngine} to {@code process(templateName, context, writer)} directly
 * onto the response writer.
 *
 * <h5 class='figure'>Usage:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 	<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 		<jk>return</jk> ThymeleafView.<jsm>of</jsm>(<js>"hello"</js>)
 * 			.attr(<js>"name"</js>, <jv>name</jv>)
 * 			.attr(<js>"ts"</js>, Instant.<jsm>now</jsm>());
 * 	}
 * </p>
 *
 * <p>
 * The template name is engine-relative &mdash; the configured
 * {@code ClassLoaderTemplateResolver} (or whatever resolver the active
 * {@code org.thymeleaf.TemplateEngine} carries) prepends its own prefix and appends its own suffix
 * (typically {@code .html}). With the bridge's default engine + {@code basePath("/templates/")},
 * {@code ThymeleafView.of("hello")} resolves to {@code /templates/hello.html} on the classpath.
 *
 * <h5 class='section'>Immutability:</h5>
 *
 * <p>
 * Each {@code attr(...)} / {@code attrs(...)} / {@code header(...)} call returns a <b>new</b>
 * {@code ThymeleafView} carrying the additional binding; the original instance is unchanged. This
 * keeps {@code ThymeleafView} a value type safe to share across requests (e.g. as a static
 * singleton built once and returned from many handlers).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ThymeleafMixin}
 * 	<li class='jc'>{@link ThymeleafViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class ThymeleafView implements View {

	private final String templateName;
	private final Map<String, Object> attributes;
	private final Map<String, String> responseHeaders;

	/**
	 * Creates a new {@code ThymeleafView} carrying the given template name and no attributes.
	 *
	 * <p>
	 * The template name is engine-relative; the configured
	 * {@code org.thymeleaf.TemplateEngine} resolver supplies the prefix and suffix. For the
	 * bridge's default engine: {@code "hello"} resolves to {@code <basePath>hello.html}.
	 *
	 * @param templateName The template name (engine-relative, no extension). Must not be
	 * 	{@code null} or blank.
	 * @return A new {@code ThymeleafView} instance.
	 * @throws IllegalArgumentException If {@code templateName} is {@code null} or blank.
	 */
	public static ThymeleafView of(String templateName) {
		if (isBlank(templateName))
			throw iaex("templateName must not be null or blank");
		return new ThymeleafView(templateName, Map.of(), Map.of());
	}

	private ThymeleafView(String templateName, Map<String, Object> attributes, Map<String, String> responseHeaders) {
		this.templateName = templateName;
		this.attributes = attributes;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * Returns a copy of this view with the given attribute added (or replaced if a binding for
	 * {@code key} already exists).
	 *
	 * <p>
	 * Each attribute is exposed to the template as a named variable (e.g.
	 * {@code th:text="${name}"} resolves the {@code "name"} key from the populated
	 * {@code Context}). {@code null} values are rejected at build-time because a
	 * Thymeleaf {@code Context} silently drops {@code null}-valued bindings, masking what is
	 * typically a caller bug; rejecting up-front surfaces the mistake at the call site.
	 *
	 * @param key The attribute key (exposed inside the template as {@code ${key}}). Must not be
	 * 	{@code null} or blank.
	 * @param value The attribute value. Must not be {@code null}.
	 * @return A new {@code ThymeleafView} carrying the additional attribute.
	 * @throws IllegalArgumentException If {@code key} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public ThymeleafView attr(String key, Object value) {
		if (isBlank(key))
			throw iaex("attribute key must not be null or blank");
		if (value == null)
			throw iaex("attribute value must not be null (attribute ''{0}'')", key);
		var copy = new LinkedHashMap<>(attributes);
		copy.put(key, value);
		return new ThymeleafView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with all entries from the given map added (or replaced if a
	 * binding for any key already exists).
	 *
	 * @param values The attributes to add. {@code null} is treated as an empty map (the result is
	 * 	the same instance with no entries added).
	 * @return A new {@code ThymeleafView} carrying the additional attributes.
	 * @throws IllegalArgumentException If any entry has a {@code null}/blank key or a {@code null}
	 * 	value.
	 */
	public ThymeleafView attrs(Map<String, ?> values) {
		if (values == null || values.isEmpty())
			return this;
		var copy = new LinkedHashMap<>(attributes);
		values.forEach((k, v) -> {
			if (isBlank(k))
				throw iaex("attribute key must not be null or blank");
			if (v == null)
				throw iaex("attribute value must not be null (attribute ''{0}'')", k);
			copy.put(k, v);
		});
		return new ThymeleafView(templateName, Map.copyOf(copy), responseHeaders);
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
	 * @return A new {@code ThymeleafView} carrying the additional response header.
	 * @throws IllegalArgumentException If {@code name} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public ThymeleafView header(String name, String value) {
		if (isBlank(name))
			throw iaex("header name must not be null or blank");
		if (value == null)
			throw iaex("header value must not be null (header ''{0}'')", name);
		var copy = new LinkedHashMap<>(responseHeaders);
		copy.put(name, value);
		return new ThymeleafView(templateName, attributes, Map.copyOf(copy));
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
		return "ThymeleafView[template=" + templateName + ", attrs=" + attributes.keySet()
			+ ", headers=" + responseHeaders.keySet() + "]";
	}
}
