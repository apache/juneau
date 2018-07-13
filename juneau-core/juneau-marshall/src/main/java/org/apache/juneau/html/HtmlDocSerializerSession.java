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

	private final HtmlDocSerializer ctx;
	private final String[] navlinks, head, header, nav, aside, footer;
	private final Set<String> style, stylesheet, script;

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
		this.ctx = ctx;

		header = getProperty(HTMLDOC_header, String[].class, ctx.getHeader());
		nav = getProperty(HTMLDOC_nav, String[].class, ctx.getNav());
		aside = getProperty(HTMLDOC_aside, String[].class, ctx.getAside());
		footer = getProperty(HTMLDOC_footer, String[].class, ctx.getFooter());
		navlinks = getProperty(HTMLDOC_navlinks, String[].class, ctx.getNavlinks());

		// These can contain dups after variable resolution, so de-dup them with hashsets.
		style = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_style, String[].class, ctx.getStyle())));
		stylesheet = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_stylesheet, String[].class, ctx.getStylesheet())));
		script = new LinkedHashSet<>(Arrays.asList(getProperty(HTMLDOC_script, String[].class, ctx.getScript())));

		head = getProperty(HTMLDOC_head, String[].class, ctx.getHead());
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
				.append("script", script)
				.append("style", style)
				.append("stylesheet", stylesheet)
			);
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
	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  CSS style code.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_style
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final Set<String> getStyle() {
		return style;
	}

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_stylesheet
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final Set<String> getStylesheet() {
		return stylesheet;
	}

	/**
	 * Configuration property:  Javascript code.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_script
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final Set<String> getScript() {
		return script;
	}

	/**
	 * Configuration property:  Page navigation links.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_navlinks
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return navlinks;
	}

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_head
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return head;
	}

	/**
	 * Configuration property:  Header section contents.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_header
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return header;
	}

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_nav
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return nav;
	}

	/**
	 * Configuration property:  Aside section contents.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_aside
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return aside;
	}

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_footer
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return footer;
	}

	/**
	 * Configuration property:  No-results message.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_noResultsMessage
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return ctx.getNoResultsMessage();
	}

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_nowrap
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return ctx.isNowrap();
	}

	/**
	 * Configuration property:  HTML document template.
	 *
	 * @see HtmlDocSerializer#HTMLDOC_template
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return ctx.getTemplate();
	}
}
