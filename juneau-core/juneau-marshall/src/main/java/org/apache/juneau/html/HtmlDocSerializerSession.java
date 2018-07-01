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

import static org.apache.juneau.html.HtmlDocSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSerializer} and its subclasses.
 *
 * <p>
 * See {@link Serializer} for details.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlDocSerializerSession extends HtmlStrippedDocSerializerSession {

	private final String noResultsMessage;
	private final String[] navlinks, head, header, nav, aside, footer;
	private final Set<String> style, stylesheet, script;
	private final boolean nowrap;
	private final HtmlDocTemplate template;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected HtmlDocSerializerSession(HtmlDocSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		header = getProperty(HTMLDOC_header, String[].class, ctx.nav);
		nav = getProperty(HTMLDOC_nav, String[].class, ctx.nav);
		aside = getProperty(HTMLDOC_aside, String[].class, ctx.aside);
		footer = getProperty(HTMLDOC_footer, String[].class, ctx.footer);
		navlinks = getProperty(HTMLDOC_navlinks, String[].class, ctx.navlinks);

		// These can contain dups after variable resolution, so de-dup them with hashsets.
		style = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_style, String[].class, ctx.style)));
		stylesheet = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_stylesheet, String[].class, ctx.stylesheet)));
		script = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_script, String[].class, ctx.script)));

		head = getProperty(HTMLDOC_head, String[].class, ctx.head);
		nowrap = getProperty(HTMLDOC_nowrap, boolean.class, ctx.nowrap);
		noResultsMessage = getProperty(HTMLDOC_noResultsMessage, String.class, ctx.noResultsMessage);
		template = getInstanceProperty(HTMLDOC_template, HtmlDocTemplate.class, ctx.template);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlDocSerializerSession", new ObjectMap()
				.append("aside", aside)
				.append("head", head)
				.append("header", header)
				.append("footer", footer)
				.append("nav", nav)
				.append("navlinks", navlinks)
				.append("noResultsMessage", noResultsMessage)
				.append("nowrap", nowrap)
				.append("script", script)
				.append("style", style)
				.append("stylesheet", stylesheet)
				.append("template", template)
			);
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_style} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_style} setting value in this context.
	 * 	An empty array if not specified.
	 * 	Never <jk>null</jk>.
	 */
	public final Set<String> getStyle() {
		return style;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_stylesheet} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_stylesheet} setting value in this context.
	 * 	An empty array if not specified.
	 * 	Never <jk>null</jk>.
	 */
	public final Set<String> getStylesheet() {
		return stylesheet;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_script} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_script} setting value in this context.
	 * 	An empty array if not specified.
	 * 	Never <jk>null</jk>.
	 */
	public final Set<String> getScript() {
		return script;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_head} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_head} setting value in this context.
	 * 	An empty array if not specified.
	 * 	Never <jk>null</jk>.
	 */
	public final String[] getHead() {
		return head;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_nowrap} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializer#HTMLDOC_nowrap} setting value in this context.
	 */
	public final boolean isNoWrap() {
		return nowrap;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_header} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_header} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	 Never an empty string.
	 */
	public final String[] getHeader() {
		return header;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_navlinks} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_navlinks} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty map.
	 */
	public final String[] getNavLinks() {
		return navlinks;
	}

	/**
	 * Returns the template to use for generating the HTML page.
	 *
	 * @return
	 * 	The HTML page generator.
	 * 	Never <jk>null</jk>.
	 */
	public final HtmlDocTemplate getTemplate() {
		return template;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_nav} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_nav} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty string.
	 */
	public final String[] getNav() {
		return nav;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_aside} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_aside} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 *  	Never an empty string.
	 */
	public final String[] getAside() {
		return aside;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_footer} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_footer} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty string.
	 */
	public final String[] getFooter() {
		return footer;
	}

	/**
	 * Returns the {@link HtmlDocSerializer#HTMLDOC_noResultsMessage} setting value in this context.
	 *
	 * @return
	 * 	The {@link HtmlDocSerializer#HTMLDOC_noResultsMessage} setting value in this context.
	 * 	<jk>null</jk> if not specified.
	 * 	Never an empty string.
	 */
	public final String getNoResultsMessage() {
		return noResultsMessage;
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {

		try (HtmlWriter w = getHtmlWriter(out)) {
			HtmlDocTemplate t = getTemplate();

			w.sTag("html").nl(0);
			w.sTag(1, "head").nl(1);
			t.head(this, w, o);
			w.eTag(1, "head").nl(1);
			w.sTag(1, "body").nl(1);
			t.body(this, w, o);
			w.eTag(1, "body").nl(1);
			w.eTag("html").nl(0);
		}
	}

	/**
	 * Calls the parent {@link #doSerialize(SerializerPipe, Object)} method which invokes just the HTML serializer.
	 *
	 * @param out
	 * 	Where to send the output from the serializer.
	 * @param o The object being serialized.
	 * @throws Exception
	 */
	public void parentSerialize(Object out, Object o) throws Exception {
		try (SerializerPipe pipe = createPipe(out)) {
			super.doSerialize(pipe, o);
		}
	}
}
