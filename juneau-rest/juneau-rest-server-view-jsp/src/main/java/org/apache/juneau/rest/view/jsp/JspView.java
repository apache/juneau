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
package org.apache.juneau.rest.view.jsp;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.rest.view.*;

/**
 * Immutable value class returned from {@code @RestOp}-annotated methods to ask the framework to
 * render a JSP template.
 *
 * <p>
 * Companion to {@link JspMixin} and {@link JspViewRenderer}: the mixin sets up the
 * {@code /jsp/*} mount and registers the renderer; the renderer detects {@code JspView} returns
 * in the response-processor chain and dispatches via
 * {@link jakarta.servlet.RequestDispatcher#forward(jakarta.servlet.ServletRequest,
 * jakarta.servlet.ServletResponse) ServletContext.getRequestDispatcher(...).forward(...)}.
 *
 * <h5 class='figure'>Usage:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/hello/{name}"</js>)
 * 	<jk>public</jk> View hello(<ja>@Path</ja> String <jv>name</jv>) {
 * 		<jk>return</jk> JspView.<jsm>of</jsm>(<js>"hello.jsp"</js>)
 * 			.attr(<js>"name"</js>, <jv>name</jv>)
 * 			.attr(<js>"ts"</js>, Instant.<jsm>now</jsm>());
 * 	}
 * </p>
 *
 * <h5 class='section'>Immutability:</h5>
 *
 * <p>
 * Each {@code attr(...)} / {@code attrs(...)} / {@code header(...)} call returns a <b>new</b>
 * {@code JspView} carrying the additional binding; the original instance is unchanged. This keeps
 * {@code JspView} a value type safe to share across requests (e.g. as a static singleton built
 * once and returned from many handlers).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JspMixin}
 * 	<li class='jc'>{@link JspViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class JspView implements View {

	private final String templateName;
	private final Map<String, Object> attributes;
	private final Map<String, String> responseHeaders;

	/**
	 * Creates a new {@code JspView} carrying the given template name and no attributes.
	 *
	 * @param templateName The template name (relative to the
	 * 	{@link JspMixin.Builder#basePath base path}). Must not be {@code null} or blank.
	 * @return A new {@code JspView} instance.
	 * @throws IllegalArgumentException If {@code templateName} is {@code null} or blank.
	 */
	public static JspView of(String templateName) {
		if (isBlank(templateName))
			throw illegalArg("templateName must not be null or blank");
		return new JspView(templateName, Map.of(), Map.of());
	}

	private JspView(String templateName, Map<String, Object> attributes, Map<String, String> responseHeaders) {
		this.templateName = templateName;
		this.attributes = attributes;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * Returns a copy of this view with the given attribute added (or replaced if a binding for
	 * {@code key} already exists).
	 *
	 * <p>
	 * Per the Servlet spec, passing {@code null} to
	 * {@link jakarta.servlet.ServletRequest#setAttribute setAttribute(name, null)} removes the
	 * binding rather than setting it; {@code JspView} mirrors that constraint at build-time so
	 * the renderer never has to short-circuit a request-attribute write at dispatch time.
	 *
	 * @param key The attribute key (exposed inside the JSP as {@code ${key}}). Must not be
	 * 	{@code null} or blank.
	 * @param value The attribute value. Must not be {@code null}.
	 * @return A new {@code JspView} carrying the additional attribute.
	 * @throws IllegalArgumentException If {@code key} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public JspView attr(String key, Object value) {
		if (isBlank(key))
			throw illegalArg("attribute key must not be null or blank");
		if (value == null)
			throw illegalArg("attribute value must not be null (attribute ''{0}'')", key);
		var copy = new LinkedHashMap<>(attributes);
		copy.put(key, value);
		return new JspView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with all entries from the given map added (or replaced if a
	 * binding for any key already exists).
	 *
	 * @param values The attributes to add. {@code null} is treated as an empty map (the result is
	 * 	logically the same instance with no entries added).
	 * @return A new {@code JspView} carrying the additional attributes.
	 */
	public JspView attrs(Map<String, ?> values) {
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
		return new JspView(templateName, Map.copyOf(copy), responseHeaders);
	}

	/**
	 * Returns a copy of this view with the given response header set on the rendered response.
	 *
	 * <p>
	 * Typical usage: {@code .header("Content-Type", "text/html; charset=UTF-8")} or
	 * {@code .header("Cache-Control", "no-store")}. The renderer applies these headers via
	 * {@link jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
	 * HttpServletResponse.setHeader(...)} before forwarding to the JSP engine.
	 *
	 * @param name The header name. Must not be {@code null} or blank.
	 * @param value The header value. Must not be {@code null}.
	 * @return A new {@code JspView} carrying the additional response header.
	 * @throws IllegalArgumentException If {@code name} is {@code null} or blank, or {@code value}
	 * 	is {@code null}.
	 */
	public JspView header(String name, String value) {
		if (isBlank(name))
			throw illegalArg("header name must not be null or blank");
		if (value == null)
			throw illegalArg("header value must not be null (header ''{0}'')", name);
		var copy = new LinkedHashMap<>(responseHeaders);
		copy.put(name, value);
		return new JspView(templateName, attributes, Map.copyOf(copy));
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
		return "JspView[template=" + templateName + ", attrs=" + attributes.keySet()
			+ ", headers=" + responseHeaders.keySet() + "]";
	}
}
