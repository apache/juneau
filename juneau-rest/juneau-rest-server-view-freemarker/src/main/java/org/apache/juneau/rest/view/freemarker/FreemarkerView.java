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
package org.apache.juneau.rest.view.freemarker;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.rest.view.*;

/**
 * Immutable value class returned from {@code @RestOp}-annotated methods to ask the framework to
 * render an Apache FreeMarker template.
 *
 * <p>
 * Companion to {@link FreemarkerMixin} and {@link FreemarkerViewRenderer}: the mixin sets
 * up the {@code /freemarker/*} mount and registers the renderer; the renderer detects
 * {@code FreemarkerView} returns in the response-processor chain and asks the configured
 * {@code freemarker.template.Configuration} to {@code getTemplate(templateName)} and then
 * {@code process(dataModel, writer)} directly onto the response writer.
 *
 * <h5 class='figure'>Usage:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 	<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 		<jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"hello.ftlh"</js>)
 * 			.attr(<js>"name"</js>, <jv>name</jv>)
 * 			.attr(<js>"ts"</js>, Instant.<jsm>now</jsm>());
 * 	}
 * </p>
 *
 * <p>
 * The template name is configuration-relative &mdash; the configured
 * {@code freemarker.template.Configuration} template loader prepends its own resource root. With
 * the bridge's default configuration + {@code basePath("/templates/")},
 * {@code FreemarkerView.of("hello.ftlh")} resolves to {@code /templates/hello.ftlh} on the
 * classpath.
 *
 * <h5 class='section'>{@code .ftl} vs {@code .ftlh} auto-escape:</h5>
 *
 * <p>
 * FreeMarker auto-selects HTML escaping by file extension: {@code .ftlh} templates emit
 * HTML-escaped output (variable references are escaped automatically), while {@code .ftl}
 * templates emit raw output. For HTML responses, prefer the {@code .ftlh} extension to avoid an
 * XSS regression sneaking in via a future attribute-binding change.
 *
 * <h5 class='section'>Template suffix:</h5>
 *
 * <p>
 * The {@link FreemarkerMixin.Builder#templateSuffix(String) templateSuffix(...)} builder
 * knob lets the bridge append a suffix (e.g. {@code ".ftlh"}) to template names that don't
 * already end with it, so callers can write {@code FreemarkerView.of("hello")} and have the
 * bridge resolve it as {@code "hello.ftlh"} on the classpath (idempotent &mdash; explicit suffix
 * is honored).
 *
 * <h5 class='section'>Immutability:</h5>
 *
 * <p>
 * Each {@code attr(...)} / {@code attrs(...)} / {@code header(...)} call returns a <b>new</b>
 * {@code FreemarkerView} carrying the additional binding; the original instance is unchanged.
 * This keeps {@code FreemarkerView} a value type safe to share across requests (e.g. as a static
 * singleton built once and returned from many handlers).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FreemarkerMixin}
 * 	<li class='jc'>{@link FreemarkerViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class FreemarkerView implements View {

	private final String templateName;
	private final Map<String, Object> attributes;
	private final Map<String, String> responseHeaders;

	/**
	 * Creates a new {@code FreemarkerView} carrying the given template name and no attributes.
	 *
	 * <p>
	 * The template name is configuration-relative; the configured
	 * {@code freemarker.template.Configuration} template loader supplies the prefix (resource
	 * root). With the bridge's default configuration + {@code basePath("/templates/")}, the name
	 * {@code "hello.ftlh"} resolves to {@code /templates/hello.ftlh} on the classpath.
	 *
	 * @param templateName The template name (configuration-relative, suffix optional when the
	 * 	mixin's {@link FreemarkerMixin.Builder#templateSuffix(String) templateSuffix} is
	 * 	configured). Must not be {@code null} or blank.
	 * @return A new {@code FreemarkerView} instance.
	 * @throws IllegalArgumentException If {@code templateName} is {@code null} or blank.
	 */
	public static FreemarkerView of(String templateName) {
		if (isBlank(templateName))
			throw illegalArg("templateName must not be null or blank");
		return new FreemarkerView(templateName, Map.of(), Map.of());
	}

	private FreemarkerView(String templateName, Map<String, Object> attributes, Map<String, String> responseHeaders) {
		this.templateName = templateName;
		this.attributes = attributes;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * Returns a copy of this view with the given attribute added (or replaced if a binding for
	 * {@code key} already exists).
	 *
	 * <p>
	 * The full attribute map is passed to FreeMarker as the data model, so each entry is
	 * accessible inside the template as {@code ${key}}. {@code null} values are rejected at
	 * build-time because FreeMarker's default null-handling renders missing/null bindings as an
	 * error or empty string depending on configuration, which typically masks what is a caller
	 * bug; rejecting up-front surfaces the mistake at the call site (matches the
	 * Thymeleaf / Mustache bridge precedents).
	 *
	 * @param key The attribute key (exposed inside the template as {@code ${key}}). Must not be
	 * 	{@code null} or blank.
	 * @param value The attribute value. Must not be {@code null}.
	 * @return A new {@code FreemarkerView} carrying the additional attribute.
	 * @throws IllegalArgumentException If {@code key} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public FreemarkerView attr(String key, Object value) {
		if (isBlank(key))
			throw illegalArg("attribute key must not be null or blank");
		if (value == null)
			throw illegalArg("attribute value must not be null (attribute ''{0}'')", key);
		var copy = new LinkedHashMap<>(attributes);
		copy.put(key, value);
		return new FreemarkerView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with all entries from the given map added (or replaced if a
	 * binding for any key already exists).
	 *
	 * @param values The attributes to add. {@code null} is treated as an empty map (the result is
	 * 	the same instance with no entries added).
	 * @return A new {@code FreemarkerView} carrying the additional attributes.
	 * @throws IllegalArgumentException If any entry has a {@code null}/blank key or a {@code null}
	 * 	value.
	 */
	public FreemarkerView attrs(Map<String, ?> values) {
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
		return new FreemarkerView(templateName, Map.copyOf(copy), responseHeaders);
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
	 * @return A new {@code FreemarkerView} carrying the additional response header.
	 * @throws IllegalArgumentException If {@code name} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public FreemarkerView header(String name, String value) {
		if (isBlank(name))
			throw illegalArg("header name must not be null or blank");
		if (value == null)
			throw illegalArg("header value must not be null (header ''{0}'')", name);
		var copy = new LinkedHashMap<>(responseHeaders);
		copy.put(name, value);
		return new FreemarkerView(templateName, attributes, Map.copyOf(copy));
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
		return "FreemarkerView[template=" + templateName + ", attrs=" + attributes.keySet()
			+ ", headers=" + responseHeaders.keySet() + "]";
	}
}
