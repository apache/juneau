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

import static java.util.Optional.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static java.util.Collections.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>,
 * <code><xt>&lt;head&gt;</code>, and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <p>
 * Configurable properties are typically specified via <ja>@HtmlDocConfig</ja>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@Rest</ja>(
 * 		messages=<js>"nls/AddressBookResource"</js>,
 * 		title=<js>"$L{title}"</js>,
 * 		description=<js>"$L{description}"</js>
 * 	)
 * 	<ja>@HtmlDocConfig</ja>(
 * 		navlinks={
 * 			<js>"api: servlet:/api"</js>,
 * 			<js>"doc: doc"</js>
 * 		}
 * 	)
 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
 * </p>
 *
 * <p>
 * The <c>$L{...}</c> variable represent localized strings pulled from the resource bundle identified by the
 * <c>messages</c> annotation.
 * <br>These variables are replaced at runtime based on the HTTP request locale.
 * <br>Several built-in runtime variable types are defined, and the API can be extended to include user-defined variables.
 */
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String[] style, stylesheet, script, navlinks, head, header, nav, aside, footer;
	final AsideFloat asideFloat;
	final String noResultsMessage;
	final boolean nowrap;
	final Class<? extends HtmlDocTemplate> template;
	final List<Class<? extends HtmlWidget>> widgets;

	private final HtmlWidgetMap widgetMap;
	private final HtmlDocTemplate templateBean;

	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlDocSerializer(HtmlDocSerializerBuilder builder) {
		super(builder);
		style = ofNullable(builder.style).map(x -> toArray(x)).orElse(new String[0]);
		stylesheet = ofNullable(builder.stylesheet).map(x -> toArray(x)).orElse(new String[0]);
		script = ofNullable(builder.script).map(x -> toArray(x)).orElse(new String[0]);
		head = ofNullable(builder.head).map(x -> toArray(x)).orElse(new String[0]);
		header = ofNullable(builder.header).map(x -> toArray(x)).orElse(new String[0]);
		nav = ofNullable(builder.nav).map(x -> toArray(x)).orElse(new String[0]);
		aside = ofNullable(builder.aside).map(x -> toArray(x)).orElse(new String[0]);
		footer = ofNullable(builder.footer).map(x -> toArray(x)).orElse(new String[0]);
		navlinks = ofNullable(builder.navlinks).map(x -> toArray(x)).orElse(new String[0]);
		asideFloat = builder.asideFloat;
		noResultsMessage = builder.noResultsMessage;
		nowrap = builder.nowrap;
		template = builder.template;
		widgets = builder.widgets == null ? emptyList() : new ArrayList<>(builder.widgets);

		templateBean = newInstance(template);
		widgetMap = new HtmlWidgetMap();
		widgets.stream().map(x -> newInstance(x)).forEach(x -> widgetMap.append(x));
	}

	@Override /* Context */
	public HtmlDocSerializerBuilder copy() {
		return new HtmlDocSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link HtmlDocSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> HtmlDocSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link HtmlDocSerializerBuilder} object.
	 */
	public static HtmlDocSerializerBuilder create() {
		return new HtmlDocSerializerBuilder();
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlDocSerializerSession(this, args);
	}

	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		int TODO;
//		if (schemaSerializer == null)
//			schemaSerializer = (HtmlSchemaDocSerializer) HtmlDocSerializer.create().type(HtmlSchemaDocSerializer.class).beanContext(getBeanContext()).build();
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Aside section contents.
	 *
	 * @see HtmlDocSerializerBuilder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return aside;
	}

	/**
	 * Float side section contents.
	 *
	 * @see HtmlDocSerializerBuilder#asideFloat(AsideFloat)
	 * @return
	 * 	How to float the aside contents on the page.
	 */
	protected final AsideFloat getAsideFloat() {
		return asideFloat;
	}

	/**
	 * Footer section contents.
	 *
	 * @see HtmlDocSerializerBuilder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return footer;
	}

	/**
	 * Additional head section content.
	 *
	 * @see HtmlDocSerializerBuilder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return head;
	}

	/**
	 * Header section contents.
	 *
	 * @see HtmlDocSerializerBuilder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return header;
	}

	/**
	 * Nav section contents.
	 *
	 * @see HtmlDocSerializerBuilder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return nav;
	}

	/**
	 * Page navigation links.
	 *
	 * @see HtmlDocSerializerBuilder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return navlinks;
	}

	/**
	 * No-results message.
	 *
	 * @see HtmlDocSerializerBuilder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return noResultsMessage;
	}

	/**
	 * Prevent word wrap on page.
	 *
	 * @see HtmlDocSerializerBuilder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return nowrap;
	}

	/**
	 * Javascript code.
	 *
	 * @see HtmlDocSerializerBuilder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return script;
	}

	/**
	 * CSS style code.
	 *
	 * @see HtmlDocSerializerBuilder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return style;
	}

	/**
	 * Stylesheet import URLs.
	 *
	 * @see HtmlDocSerializerBuilder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return stylesheet;
	}

	/**
	 * HTML document template.
	 *
	 * @see HtmlDocSerializerBuilder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return templateBean;
	}

	/**
	 * HTML widgets.
	 *
	 * @see HtmlDocSerializerBuilder#widgets(Class...)
	 * @return
	 * 	Widgets defined on this serializers.
	 */
	protected final HtmlWidgetMap getWidgets() {
		return widgetMap;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private String[] toArray(List<String> x) {
		return x.toArray(new String[x.size()]);
	}

	private <T> T newInstance(Class<T> c) {
		try {
			return c.newInstance();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlDocSerializer",
				OMap
					.create()
					.filtered()
					.a("header", header)
					.a("nav", nav)
					.a("navlinks", navlinks)
					.a("aside", aside)
					.a("asideFloat", asideFloat)
					.a("footer", footer)
					.a("style", style)
					.a("head", head)
					.a("stylesheet", stylesheet)
					.a("nowrap", nowrap)
					.a("template", template)
					.a("noResultsMessage", noResultsMessage)
					.a("widgets", widgets)
			);
	}
}
