// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSerializer} and its subclasses.
 * <p>
 * See {@link SerializerContext} for details.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class HtmlDocSerializerSession extends HtmlSerializerSession {

	private final String title, text, cssUrl;
	private final String[] cssImports;
	private final Map<String,String> links;
	private final boolean nowrap;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this serializer, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @param uriContext The URI context.
	 * 	Identifies the current request URI used for resolution of URIs to absolute or root-relative form.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected HtmlDocSerializerSession(HtmlDocSerializerContext ctx, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
		if (op == null || op.isEmpty()) {
			title = ctx.title;
			text = ctx.text;
			links = ctx.links;
			cssUrl = ctx.cssUrl;
			cssImports = ctx.cssImports;
			nowrap = ctx.nowrap;
		} else {
			title = op.getString(HTMLDOC_title, ctx.title);
			text = op.getString(HTMLDOC_text, ctx.text);
			links = new LinkedHashMap(op.getMap(HTMLDOC_links, ctx.links));
			cssUrl = op.getString(HTMLDOC_cssUrl, ctx.cssUrl);
			cssImports = split(op.getString(HTMLDOC_cssImports, null), ',');
			nowrap = op.getBoolean(HTMLDOC_cssUrl, ctx.nowrap);
		}
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_title} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_title} setting value in this context.
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_text} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_text} setting value in this context.
	 */
	public final String getText() {
		return text;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_links} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_links} setting value in this context.
	 */
	public final Map<String,String> getLinks() {
		return links;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_cssUrl} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_cssUrl} setting value in this context.
	 */
	public final String getCssUrl() {
		return cssUrl;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_cssImports} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_cssImports} setting value in this context.
	 */
	public final String[] getCssImports() {
		return cssImports;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_nowrap} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_nowrap} setting value in this context.
	 */
	public final boolean isNoWrap() {
		return nowrap;
	}

	@Override /* XmlSerializerSession */
	public HtmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof HtmlWriter)
			return (HtmlWriter)output;
		return new HtmlWriter(super.getWriter(), isUseWhitespace(), isTrimStrings(), getQuoteChar(), getUriResolver());
	}
}
