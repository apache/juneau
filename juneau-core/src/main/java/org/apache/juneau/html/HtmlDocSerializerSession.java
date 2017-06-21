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
import org.apache.juneau.internal.*;
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

	private final String title, description, branding, header, nav, aside, footer, cssUrl, noResultsMessage;
	private final String[] css;
	private final Map<String,Object> links;
	private final boolean nowrap;
	private final HtmlDocTemplate template;

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
			description = ctx.description;
			branding = ctx.branding;
			header = ctx.header;
			nav = ctx.nav;
			aside = ctx.aside;
			footer = ctx.footer;
			links = ctx.links;
			cssUrl = ctx.cssUrl;
			css = ctx.css;
			nowrap = ctx.nowrap;
			noResultsMessage = ctx.noResultsMessage;
			template = ClassUtils.newInstance(HtmlDocTemplate.class, ctx.template);
		} else {
			title = op.getString(HTMLDOC_title, ctx.title);
			description = op.getString(HTMLDOC_description, ctx.description);
			branding = op.getString(HTMLDOC_branding, ctx.branding);
			header = op.getString(HTMLDOC_header, ctx.nav);
			nav = op.getString(HTMLDOC_nav, ctx.nav);
			aside = op.getString(HTMLDOC_aside, ctx.aside);
			footer = op.getString(HTMLDOC_footer, ctx.footer);
			Map m = op.getMap(HTMLDOC_links, ctx.links);
			links = ObjectUtils.isEmpty(m) ? null : new LinkedHashMap(m);
			cssUrl = op.getString(HTMLDOC_cssUrl, ctx.cssUrl);
			css = split(op.getString(HTMLDOC_css, null));
			nowrap = op.getBoolean(HTMLDOC_nowrap, ctx.nowrap);
			noResultsMessage = op.getString(HTMLDOC_noResultsMessage, ctx.noResultsMessage);
			template = ClassUtils.newInstance(HtmlDocTemplate.class, op.get(HTMLDOC_template, ctx.template));
		}
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_cssUrl} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_cssUrl} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getCssUrl() {
		return cssUrl;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_css} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_css} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty array.
	 */
	public final String[] getCss() {
		return css;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_nowrap} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_nowrap} setting value in this context.
	 */
	public final boolean isNoWrap() {
		return nowrap;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_title} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_title} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_description} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_description} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_branding} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_branding} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getBranding() {
		return branding;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_header} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_header} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getHeader() {
		return header;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_links} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_links} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty map.
	 */
	public final Map<String,Object> getLinks() {
		return links;
	}

	/**
	 * Returns the template to use for generating the HTML page.
	 * @return The HTML page generator.
	 * 	Never <jk>null</jk>.
	 */
	public final HtmlDocTemplate getTemplate() {
		return template;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_nav} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_nav} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getNav() {
		return nav;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_aside} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_aside} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getAside() {
		return aside;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_footer} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_footer} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getFooter() {
		return footer;
	}

	/**
	 * Returns the {@link HtmlDocSerializerContext#HTMLDOC_noResultsMessage} setting value in this context.
	 * @return The {@link HtmlDocSerializerContext#HTMLDOC_noResultsMessage} setting value in this context.
	 * 	<jk>null</jk> if not specified.  Never an empty string.
	 */
	public final String getNoResultsMessage() {
		return noResultsMessage;
	}

	@Override /* XmlSerializerSession */
	public HtmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof HtmlWriter)
			return (HtmlWriter)output;
		return new HtmlWriter(super.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getQuoteChar(), getUriResolver());
	}
}
