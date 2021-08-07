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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.annotation.*;
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
@ConfigurableContext
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "HtmlDocSerializer";

	/**
	 * Configuration property:  Aside section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the aside section on the HTML page.
	 * The aside section floats on the right of the page for providing content supporting the serialized content of
	 * the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_aside HTMLDOC_aside}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.aside.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.aside</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_ASIDE</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#aside()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#aside(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_aside = PREFIX + ".aside.ls";

	/**
	 * Configuration property:  Float aside section contents.
	 *
	 * <p>
	 * Allows you to position the aside contents of the page around the main contents.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_asideFloat HTMLDOC_asideFloat}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.asideFloat.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.html.AsideFloat}
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.asideFloat</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_ASIDEFLOAT</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.html.AsideFloat#DEFAULT}
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#asideFloat()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#asideFloat(AsideFloat)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_asideFloat = PREFIX + ".asideFloat.s";

	/**
	 * Configuration property:  Footer section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the footer section on the HTML page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_footer HTMLDOC_footer}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.footer.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.footer</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_FOOTER</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#footer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#footer(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_footer = PREFIX + ".footer.ls";

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_head HTMLDOC_head}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.head.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.head</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_HEAD</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#head()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#head(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_head = PREFIX + ".head.ls";

	/**
	 * Configuration property:  Header section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the header section on the HTML page.
	 * The header section normally contains the title and description at the top of the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_header HTMLDOC_header}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.header.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.header</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_HEADER</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#header()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#header(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_header = PREFIX + ".header.ls";

	/**
	 * Configuration property:  Nav section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the nav section on the HTML page.
	 * The nav section normally contains the page links at the top of the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_nav HTMLDOC_nav}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.nav.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.nav</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_NAV</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#nav()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#nav(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_nav = PREFIX + ".nav.ls";

	/**
	 * Configuration property:  Page navigation links.
	 *
	 * <p>
	 * Adds a list of hyperlinks immediately under the title and description but above the content of the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_navlinks HTMLDOC_navlinks}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.navlinks.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.navlinks</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_NAVLINKS</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#navlinks()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#navlinks(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_navlinks = PREFIX + ".navlinks.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_navlinks} property.
	 */
	public static final String HTMLDOC_navlinks_add = PREFIX + ".navlinks.ls/add";

	/**
	 * Configuration property:  No-results message.
	 *
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_noResultsMessage HTMLDOC_noResultsMessage}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.noResultsMessage.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.noResultsMessage</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_NORESULTSMESSAGE</c>
	 * 	<li><b>Default:</b>  <js>"&lt;p&gt;no results&lt;/p&gt;"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#noResultsMessage()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#noResultsMessage(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_noResultsMessage = PREFIX + ".noResultsMessage.s";

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_nowrap HTMLDOC_nowrap}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.nowrap.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.nowrap</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_NOWRAP</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#nowrap()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#nowrap()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_nowrap = PREFIX + ".nowrap.b";

	/**
	 * Configuration property:  Javascript code.
	 *
	 * <p>
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_script HTMLDOC_script}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.script.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.script</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_SCRIPT</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#script()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#script(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_script = PREFIX + ".script.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_script} property.
	 */
	public static final String HTMLDOC_script_add = PREFIX + ".script.ls/add";

	/**
	 * Configuration property:  CSS style code.
	 *
	 * <p>
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_style HTMLDOC_style}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.style.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.style</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_STYLE</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#style()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#style(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_style = PREFIX + ".style.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_style} property.
	 */
	public static final String HTMLDOC_style_add = PREFIX + ".style.ls/add";

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_stylesheet HTMLDOC_stylesheet}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.stylesheet.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>HtmlDocSerializer.stylesheet</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLDOCSERIALIZER_STYLESHEET</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#stylesheet()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#stylesheet(String[])}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_stylesheet = PREFIX + ".stylesheet.ls";

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_stylesheet} property.
	 */
	public static final String HTMLDOC_stylesheet_add = PREFIX + ".stylesheet.ls/add";

	/**
	 * Configuration property:  HTML document template.
	 *
	 * <p>
	 * Specifies the template to use for serializing the page.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_template HTMLDOC_template}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.template.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;{@link org.apache.juneau.html.HtmlDocTemplate}&gt;</code>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.html.BasicHtmlDocTemplate}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#template()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#template(Class)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_template = PREFIX + ".template.c";

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlDocSerializer#HTMLDOC_widgets HTMLDOC_widgets}
	 * 	<li><b>Name:</b>  <js>"HtmlDocSerializer.widgets.lo"</js>
	 * 	<li><b>Data type:</b><c>List&lt;{@link org.apache.juneau.html.HtmlWidget}|Class&lt;{@link org.apache.juneau.html.HtmlWidget}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link HtmlDocConfig#widgets()}
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlDocConfig#widgets()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link HtmlDocSerializerBuilder#widgets(Class...)}
	 * 			<li class='jm'>{@link HtmlDocSerializerBuilder#widgets(HtmlWidget...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTMLDOC_widgets = PREFIX + ".widgets.lo";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(ContextProperties.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String[] style, stylesheet, script, navlinks, head, header, nav, aside, footer;
	private final AsideFloat asideFloat;
	private final String noResultsMessage;
	private final boolean nowrap;
	private final HtmlDocTemplate template;
	private final HtmlWidgetMap widgets;

	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param cp The property store containing all the settings for this object.
	 */
	public HtmlDocSerializer(ContextProperties cp) {
		this(cp, "text/html", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of
	 * 	{@doc ExtRFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json",text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public HtmlDocSerializer(ContextProperties cp, String produces, String accept) {
		super(cp, produces, accept);
		style = cp.getArray(HTMLDOC_style, String.class).orElse(new String[0]);
		stylesheet = cp.getArray(HTMLDOC_stylesheet, String.class).orElse(new String[0]);
		script = cp.getArray(HTMLDOC_script, String.class).orElse(new String[0]);
		head = cp.getArray(HTMLDOC_head, String.class).orElse(new String[0]);
		header = cp.getArray(HTMLDOC_header, String.class).orElse(new String[0]);
		nav = cp.getArray(HTMLDOC_nav, String.class).orElse(new String[0]);
		aside = cp.getArray(HTMLDOC_aside, String.class).orElse(new String[0]);
		asideFloat = cp.get(HTMLDOC_asideFloat, AsideFloat.class).orElse(AsideFloat.RIGHT);
		footer = cp.getArray(HTMLDOC_footer, String.class).orElse(new String[0]);
		nowrap = cp.getBoolean(HTMLDOC_nowrap).orElse(false);
		navlinks = cp.getArray(HTMLDOC_navlinks, String.class).orElse(new String[0]);
		noResultsMessage = cp.getString(HTMLDOC_noResultsMessage).orElse("<p>no results</p>");
		template = cp.getInstance(HTMLDOC_template, HtmlDocTemplate.class).orElseGet(BasicHtmlDocTemplate::new);

		widgets = new HtmlWidgetMap();
		widgets.append(cp.getInstanceArray(HTMLDOC_widgets, HtmlWidget.class).orElse(new HtmlWidget[0]));
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
		if (schemaSerializer == null)
			schemaSerializer = copy().build(HtmlSchemaDocSerializer.class);
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Aside section contents.
	 *
	 * @see #HTMLDOC_aside
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return aside;
	}

	/**
	 * Float side section contents.
	 *
	 * @see #HTMLDOC_asideFloat
	 * @return
	 * 	How to float the aside contents on the page.
	 */
	protected final AsideFloat getAsideFloat() {
		return asideFloat;
	}

	/**
	 * Footer section contents.
	 *
	 * @see #HTMLDOC_footer
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return footer;
	}

	/**
	 * Additional head section content.
	 *
	 * @see #HTMLDOC_head
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return head;
	}

	/**
	 * Header section contents.
	 *
	 * @see #HTMLDOC_header
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return header;
	}

	/**
	 * Nav section contents.
	 *
	 * @see #HTMLDOC_nav
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return nav;
	}

	/**
	 * Page navigation links.
	 *
	 * @see #HTMLDOC_navlinks
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return navlinks;
	}

	/**
	 * No-results message.
	 *
	 * @see #HTMLDOC_noResultsMessage
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return noResultsMessage;
	}

	/**
	 * Prevent word wrap on page.
	 *
	 * @see #HTMLDOC_nowrap
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return nowrap;
	}

	/**
	 * Javascript code.
	 *
	 * @see #HTMLDOC_script
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return script;
	}

	/**
	 * CSS style code.
	 *
	 * @see #HTMLDOC_style
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return style;
	}

	/**
	 * Stylesheet import URLs.
	 *
	 * @see #HTMLDOC_stylesheet
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return stylesheet;
	}

	/**
	 * HTML document template.
	 *
	 * @see #HTMLDOC_template
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return template;
	}

	/**
	 * HTML widgets.
	 *
	 * @see #HTMLDOC_widgets
	 * @return
	 * 	Widgets defined on this serializers.
	 */
	protected final HtmlWidgetMap getWidgets() {
		return widgets;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
					.a("widgets", widgets.keySet())
			);
	}
}
