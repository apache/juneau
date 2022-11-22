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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to HTTP responses as HTML documents.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/html</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Same as {@link HtmlSerializer}, except wraps the response in <code><xt>&lt;html&gt;</code>,
 * <code><xt>&lt;head&gt;</code>, and <code><xt>&lt;body&gt;</code> tags so that it can be rendered in a browser.
 *
 * <p>
 * Configurable properties are typically specified via <ja>@HtmlDocConfig</ja>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
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
 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServlet {
 * </p>
 *
 * <p>
 * The <c>$L{...}</c> variable represent localized strings pulled from the resource bundle identified by the
 * <c>messages</c> annotation.
 * <br>These variables are replaced at runtime based on the HTTP request locale.
 * <br>Several built-in runtime variable types are defined, and the API can be extended to include user-defined variables.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final String[] EMPTY_ARRAY = new String[0];

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends HtmlStrippedDocSerializer.Builder {

		private static final Cache<HashKey,HtmlDocSerializer> CACHE = Cache.of(HashKey.class, HtmlDocSerializer.class).build();

		List<String> aside, footer, head, header, nav, navlinks, script, style, stylesheet;
		AsideFloat asideFloat;
		String noResultsMessage;
		boolean nowrap;
		Class<? extends HtmlDocTemplate> template;
		List<Class<? extends HtmlWidget>> widgets;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("text/html");
			accept("text/html");
			asideFloat = AsideFloat.RIGHT;
			noResultsMessage = "<p>no results</p>";
			template = BasicHtmlDocTemplate.class;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(HtmlDocSerializer copyFrom) {
			super(copyFrom);
			aside = copy(copyFrom.aside);
			footer = copy(copyFrom.footer);
			head = copy(copyFrom.head);
			header = copy(copyFrom.header);
			nav = copy(copyFrom.nav);
			navlinks = copy(copyFrom.navlinks);
			script = copy(copyFrom.script);
			style = copy(copyFrom.style);
			stylesheet = copy(copyFrom.stylesheet);
			asideFloat = copyFrom.asideFloat;
			noResultsMessage = copyFrom.noResultsMessage;
			nowrap = copyFrom.nowrap;
			template = copyFrom.template;
			widgets = copy(copyFrom.widgets);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			aside = copy(copyFrom.aside);
			footer = copy(copyFrom.footer);
			head = copy(copyFrom.head);
			header = copy(copyFrom.header);
			nav = copy(copyFrom.nav);
			navlinks = copy(copyFrom.navlinks);
			script = copy(copyFrom.script);
			style = copy(copyFrom.style);
			stylesheet = copy(copyFrom.stylesheet);
			asideFloat = copyFrom.asideFloat;
			noResultsMessage = copyFrom.noResultsMessage;
			nowrap = copyFrom.nowrap;
			template = copyFrom.template;
			widgets = copy(copyFrom.widgets);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public HtmlDocSerializer build() {
			return cache(CACHE).build(HtmlDocSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				aside,
				footer,
				head,
				header,
				nav,
				navlinks,
				script,
				style,
				stylesheet,
				asideFloat,
				noResultsMessage,
				nowrap,
				template,
				widgets
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Aside section contents.
		 *
		 * <p>
		 * Allows you to specify the contents of the aside section on the HTML page.
		 * The aside section floats on the right of the page for providing content supporting the serialized content of
		 * the page.
		 *
		 * <p>
		 * By default, the aside section is empty.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.aside(
		 * 			<js>"&lt;ul&gt;"</js>,
		 * 			<js>"	&lt;li&gt;Item 1"</js>,
		 * 			<js>"	&lt;li&gt;Item 2"</js>,
		 * 			<js>"	&lt;li&gt;Item 3"</js>,
		 * 			<js>"&lt;/ul&gt;"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Format: HTML
		 * 	<li class='note'>
		 * 		Supports <a class="doclink" href="../../../../index.html#jrs.SvlVariables">SVL Variables</a>
		 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
		 * 	<li class='note'>
		 * 		A value of <js>"NONE"</js> can be used to force no value.
		 * 	<li class='note'>
		 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
		 * 	<li class='note'>
		 * 		Multiple values are combined with newlines into a single string.
		 * 	<li class='note'>
		 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
		 * 	<li class='note'>
		 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
		 * 		parent class.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder aside(String...value) {
			aside = merge(aside, value);
			return this;
		}

		/**
		 * Returns the list of aside section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #aside(String...)}.
		 *
		 * @return The list of aside section contents.
		 * @see #aside(String...)
		 */
		public List<String> aside() {
			if (aside == null)
				aside = list();
			return aside;
		}

		/**
		 * Float aside section contents.
		 *
		 * <p>
		 * Allows you to position the aside contents of the page around the main contents.
		 *
		 * <p>
		 * By default, the aside section is floated to the right.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.aside(
		 * 			<js>"&lt;ul&gt;"</js>,
		 * 			<js>"	&lt;li&gt;Item 1"</js>,
		 * 			<js>"	&lt;li&gt;Item 2"</js>,
		 * 			<js>"	&lt;li&gt;Item 3"</js>,
		 * 			<js>"&lt;/ul&gt;"</js>
		 * 		)
		 * 		.asideFloat(<jsf>RIGHT</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder asideFloat(AsideFloat value) {
			asideFloat = value;
			return this;
		}

		/**
		 * Footer section contents.
		 *
		 * <p>
		 * Allows you to specify the contents of the footer section on the HTML page.
		 *
		 * <p>
		 * By default, the footer section is empty.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.footer(
		 * 			<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder footer(String...value) {
			footer = merge(footer, value);
			return this;
		}

		/**
		 * Returns the list of footer section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #footer(String...)}.
		 *
		 * @return The list of footer section contents.
		 * @see #footer(String...)
		 */
		public List<String> footer() {
			if (footer == null)
				footer = list();
			return footer;
		}

		/**
		 * Additional head section content.
		 *
		 * <p>
		 * Adds the specified HTML content to the head section of the page.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.head(
		 * 			<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder head(String...value) {
			head = merge(head, value);
			return this;
		}

		/**
		 * Returns the list of head section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #head(String...)}.
		 *
		 * @return The list of head section contents.
		 * @see #head(String...)
		 */
		public List<String> head() {
			if (head == null)
				head = list();
			return head;
		}

		/**
		 * Header section contents.
		 *
		 * <p>
		 * Allows you to override the contents of the header section on the HTML page.
		 * The header section normally contains the title and description at the top of the page.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.header(
		 * 			<js>"&lt;h1&gt;My own header&lt;/h1&gt;"</js>
		 * 		)
		 * 		.build()
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder header(String...value) {
			header = merge(header, value);
			return this;
		}

		/**
		 * Returns the list of header section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #header(String...)}.
		 *
		 * @return The list of header section contents.
		 * @see #header(String...)
		 */
		public List<String> header() {
			if (header == null)
				header = list();
			return header;
		}

		/**
		 * Nav section contents.
		 *
		 * <p>
		 * Allows you to override the contents of the nav section on the HTML page.
		 * The nav section normally contains the page links at the top of the page.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.nav(
		 * 			<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
		 * 		)
		 * 		.build()
		 * </p>
		 *
		 * <p>
		 * When this property is specified, the {@link Builder#navlinks(String...)} property is ignored.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder nav(String...value) {
			nav = merge(nav, value);
			return this;
		}

		/**
		 * Returns the list of nav section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #nav(String...)}.
		 *
		 * @return The list of nav section contents.
		 * @see #nav(String...)
		 */
		public List<String> nav() {
			if (nav == null)
				nav = list();
			return nav;
		}

		/**
		 * Page navigation links.
		 *
		 * <p>
		 * Adds a list of hyperlinks immediately under the title and description but above the content of the page.
		 *
		 * <p>
		 * This can be used to provide convenient hyperlinks when viewing the REST interface from a browser.
		 *
		 * <p>
		 * The value is an array of strings with two possible values:
		 * <ul>
		 * 	<li>A key-value pair representing a hyperlink label and href:
		 * 		<br><js>"google: http://google.com"</js>
		 * 	<li>Arbitrary HTML.
		 * </ul>
		 *
		 * <p>
		 * Relative URLs are considered relative to the servlet path.
		 * For example, if the servlet path is <js>"http://localhost/myContext/myServlet"</js>, and the
		 * URL is <js>"foo"</js>, the link becomes <js>"http://localhost/myContext/myServlet/foo"</js>.
		 * Absolute (<js>"/myOtherContext/foo"</js>) and fully-qualified (<js>"http://localhost2/foo"</js>) URLs
		 * can also be used in addition to various other protocols specified by {@link UriResolver} such as
		 * <js>"servlet:/..."</js>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.navlinks(
		 * 			<js>"api: servlet:/api"</js>,
		 * 			<js>"stats: servlet:/stats"</js>,
		 * 			<js>"doc: doc"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder navlinks(String...value) {
			navlinks = mergeNavLinks(navlinks, value);
			return this;
		}

		/**
		 * Returns the list of navlinks section contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #navlinks(String...)}.
		 *
		 * @return The list of navlinks section contents.
		 * @see #navlinks(String...)
		 */
		public List<String> navlinks() {
			if (navlinks == null)
				navlinks = list();
			return navlinks;
		}

		/**
		 * No-results message.
		 *
		 * <p>
		 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.noResultsMessage(<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * A value of <js>"NONE"</js> can be used to represent no value to differentiate it from an empty string.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder noResultsMessage(String value) {
			noResultsMessage = value;
			return this;
		}

		/**
		 * Prevent word wrap on page.
		 *
		 * <p>
		 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder nowrap() {
			return nowrap(true);
		}

		/**
		 * Same as {@link #nowrap()} but allows you to explicitly specify the boolean value.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 * @see #nowrap()
		 */
		@FluentSetter
		public Builder nowrap(boolean value) {
			nowrap = value;
			return this;
		}

		/**
		 * Adds the specified Javascript code to the HTML page.
		 *
		 * <p>
		 * A shortcut on <ja>@Rest</ja> is also provided for this setting:
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.script(<js>"alert('hello!');"</js>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The value to add to this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder script(String...value) {
			script = merge(script, value);
			return this;
		}

		/**
		 * Returns the list of page script contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #script(String...)}.
		 *
		 * @return The list of page script contents.
		 * @see #script(String...)
		 */
		public List<String> script() {
			if (script == null)
				script = list();
			return script;
		}

		/**
		 * Adds the specified CSS instructions to the HTML page.
		 *
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.style(
		 * 			<js>"h3 { color: red; }"</js>,
		 * 			<js>"h5 { font-weight: bold; }"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The value to add to this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder style(String...value) {
			style = merge(style, value);
			return this;
		}

		/**
		 * Returns the list of page style contents.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #style(String...)}.
		 *
		 * @return The list of page style contents.
		 * @see #style(String...)
		 */
		public List<String> style() {
			if (style == null)
				style = list();
			return style;
		}

		/**
		 * Adds to the list of stylesheet URLs.
		 *
		 * <p>
		 * Note that this stylesheet is controlled by the <code><ja>@Rest</ja>.stylesheet()</code> annotation.
		 *
		 * @param value
		 * 	The value to add to this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder stylesheet(String...value) {
			stylesheet = merge(stylesheet, value);
			return this;
		}

		/**
		 * Returns the list of stylesheet URLs.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #stylesheet(String...)}.
		 *
		 * @return The list of stylesheet URLs.
		 * @see #stylesheet(String...)
		 */
		public List<String> stylesheet() {
			if (stylesheet == null)
				stylesheet = list();
			return stylesheet;
		}

		/**
		 * HTML document template.
		 *
		 * <p>
		 * Specifies the template to use for serializing the page.
		 *
		 * <p>
		 * By default, the {@link BasicHtmlDocTemplate} class is used to construct the contents of the HTML page, but
		 * can be overridden with your own custom implementation class.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.template(MySpecialDocTemplate.<jk>class</jk>)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder template(Class<? extends HtmlDocTemplate> value) {
			template = value;
			return this;
		}

		/**
		 * HTML Widgets.
		 *
		 * <p>
		 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
		 * generate arbitrary replacement text.
		 *
		 * Widgets resolve the following variables:
		 * <ul class='spaced-list'>
		 * 	<li><js>"$W{name}"</js> - Contents returned by {@link HtmlWidget#getHtml(VarResolverSession)}.
		 * 	<li><js>"$W{name.script}"</js> - Contents returned by {@link HtmlWidget#getScript(VarResolverSession)}.
		 * 		<br>The script contents are automatically inserted into the <xt>&lt;head/script&gt;</xt> section
		 * 			 in the HTML page.
		 * 	<li><js>"$W{name.style}"</js> - Contents returned by {@link HtmlWidget#getStyle(VarResolverSession)}.
		 * 		<br>The styles contents are automatically inserted into the <xt>&lt;head/style&gt;</xt> section
		 * 			 in the HTML page.
		 * </ul>
		 *
		 * <p>
		 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
		 * and aside section of the page:
		 *
		 * <p class='bjava'>
		 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.widgets(
		 * 			MyWidget.<jk>class</jk>
		 * 		)
		 * 		.navlinks(
		 * 			<js>"$W{MyWidget}"</js>
		 * 		)
		 * 		.aside(
		 * 			<js>"Check out this widget:  $W{MyWidget}"</js>
		 * 		)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Widgets are inherited from super classes, but can be overridden by reusing the widget name.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HtmlWidgets">Widgets</a>
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * @return This object.
		 */
		@FluentSetter
		@SuppressWarnings("unchecked")
		public Builder widgets(Class<? extends HtmlWidget>...values) {
			addAll(widgets(), values);
			return this;
		}

		/**
		 * Returns the list of page widgets.
		 *
		 * <p>
		 * Gives access to the inner list if you need to make more than simple additions via {@link #widgets(Class...)}.
		 *
		 * @return The list of page widgets.
		 * @see #widgets(Class...)
		 */
		public List<Class<? extends HtmlWidget>> widgets() {
			if (widgets == null)
				widgets = list();
			return widgets;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addBeanTypesXml() {
			super.addBeanTypesXml();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addBeanTypesXml(boolean value) {
			super.addBeanTypesXml(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addNamespaceUrisToRoot() {
			super.addNamespaceUrisToRoot();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addNamespaceUrisToRoot(boolean value) {
			super.addNamespaceUrisToRoot(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder defaultNamespace(Namespace value) {
			super.defaultNamespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder disableAutoDetectNamespaces() {
			super.disableAutoDetectNamespaces();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder disableAutoDetectNamespaces(boolean value) {
			super.disableAutoDetectNamespaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder enableNamespaces() {
			super.enableNamespaces();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder enableNamespaces(boolean value) {
			super.enableNamespaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder namespaces(Namespace...values) {
			super.namespaces(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder ns() {
			super.ns();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder addBeanTypesHtml() {
			super.addBeanTypesHtml();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder addBeanTypesHtml(boolean value) {
			super.addBeanTypesHtml(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder addKeyValueTableHeaders() {
			super.addKeyValueTableHeaders();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder addKeyValueTableHeaders(boolean value) {
			super.addKeyValueTableHeaders(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder disableDetectLabelParameters() {
			super.disableDetectLabelParameters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder disableDetectLabelParameters(boolean value) {
			super.disableDetectLabelParameters(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder disableDetectLinksInStrings() {
			super.disableDetectLinksInStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder disableDetectLinksInStrings(boolean value) {
			super.disableDetectLinksInStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder labelParameter(String value) {
			super.labelParameter(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.html.HtmlSerializer.Builder */
		public Builder uriAnchorText(AnchorText value) {
			super.uriAnchorText(value);
			return this;
		}

		// </FluentSetters>

		//-----------------------------------------------------------------------------------------------------------------
		// Helpers
		//-----------------------------------------------------------------------------------------------------------------

		private static <T> List<T> copy(List<T> s) {
			return s == null || s.isEmpty() ? null : copyOf(s);
		}

		private static <T> List<T> copy(T[] s) {
			return s.length == 0 ? null : list(s);
		}

		private List<String> merge(List<String> old, String[] newValues) {
			List<String> x = list(newValues.length);
			for (String s : newValues) {
				if ("NONE".equals(s)) {
					if (old != null)
						old.clear();
				} else if ("INHERIT".equals(s)) {
					if (old != null)
						x.addAll(old);
				} else {
					x.add(s);
				}
			}
			return x;
		}

		private List<String> mergeNavLinks(List<String> old, String[] newValues) {
			List<String> x = list(newValues.length);
			for (String s : newValues) {
				if ("NONE".equals(s)) {
					if (old != null)
						old.clear();
				} else if ("INHERIT".equals(s)) {
					if (old != null)
						x.addAll(old);
				} else if (s.indexOf('[') != -1 && INDEXED_LINK_PATTERN.matcher(s).matches()) {
					Matcher lm = INDEXED_LINK_PATTERN.matcher(s);
					lm.matches();
					String key = lm.group(1);
					int index = Math.min(x.size(), Integer.parseInt(lm.group(2)));
					String remainder = lm.group(3);
					x.add(index, key.isEmpty() ? remainder : key + ":" + remainder);
				} else {
					x.add(s);
				}
			}
			return x;
		}

		private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");
	}

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
	private final HtmlWidget[] widgetArray;
	private final HtmlDocTemplate templateBean;

	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public HtmlDocSerializer(Builder builder) {
		super(builder);
		style = builder.style != null ? toArray(builder.style) : EMPTY_ARRAY;
		stylesheet = builder.stylesheet != null ? toArray(builder.stylesheet) : EMPTY_ARRAY;
		script = builder.script != null ? toArray(builder.script) : EMPTY_ARRAY;
		head = builder.head != null ? toArray(builder.head) : EMPTY_ARRAY;
		header = builder.header != null ? toArray(builder.header) : EMPTY_ARRAY;
		nav = builder.nav != null ? toArray(builder.nav) : EMPTY_ARRAY;
		aside = builder.aside != null ? toArray(builder.aside) : EMPTY_ARRAY;
		footer = builder.footer != null ? toArray(builder.footer) : EMPTY_ARRAY;
		navlinks = builder.navlinks != null ? toArray(builder.navlinks) : EMPTY_ARRAY;
		asideFloat = builder.asideFloat;
		noResultsMessage = builder.noResultsMessage;
		nowrap = builder.nowrap;
		template = builder.template;
		widgets = builder.widgets == null ? emptyList() : copyOf(builder.widgets);

		templateBean = newInstance(template);
		widgetMap = new HtmlWidgetMap();
		widgets.stream().map(x -> newInstance(x)).forEach(x -> widgetMap.append(x));
		widgetArray = array(widgetMap.values(), HtmlWidget.class);
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public HtmlDocSerializerSession.Builder createSession() {
		return HtmlDocSerializerSession.create(this);
	}

	@Override /* Context */
	public HtmlDocSerializerSession getSession() {
		return createSession().build();
	}

	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = HtmlSchemaDocSerializer.create().beanContext(getBeanContext()).build();
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Aside section contents.
	 *
	 * @see Builder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() {
		return aside;
	}

	/**
	 * Float side section contents.
	 *
	 * @see Builder#asideFloat(AsideFloat)
	 * @return
	 * 	How to float the aside contents on the page.
	 */
	protected final AsideFloat getAsideFloat() {
		return asideFloat;
	}

	/**
	 * Footer section contents.
	 *
	 * @see Builder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() {
		return footer;
	}

	/**
	 * Additional head section content.
	 *
	 * @see Builder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() {
		return head;
	}

	/**
	 * Header section contents.
	 *
	 * @see Builder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() {
		return header;
	}

	/**
	 * Nav section contents.
	 *
	 * @see Builder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() {
		return nav;
	}

	/**
	 * Page navigation links.
	 *
	 * @see Builder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() {
		return navlinks;
	}

	/**
	 * No-results message.
	 *
	 * @see Builder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() {
		return noResultsMessage;
	}

	/**
	 * Prevent word wrap on page.
	 *
	 * @see Builder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() {
		return nowrap;
	}

	/**
	 * Javascript code.
	 *
	 * @see Builder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() {
		return script;
	}

	/**
	 * CSS style code.
	 *
	 * @see Builder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() {
		return style;
	}

	/**
	 * Stylesheet import URLs.
	 *
	 * @see Builder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() {
		return stylesheet;
	}

	/**
	 * HTML document template.
	 *
	 * @see Builder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() {
		return templateBean;
	}

	/**
	 * HTML widgets.
	 *
	 * @see Builder#widgets(Class...)
	 * @return
	 * 	Widgets defined on this serializers.
	 */
	protected final HtmlWidgetMap getWidgets() {
		return widgetMap;
	}

	/**
	 * Performs an action on all widgets defined on this serializer.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	protected final HtmlDocSerializer forEachWidget(Consumer<HtmlWidget> action) {
		for (HtmlWidget w : widgetArray)
			action.accept(w);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private String[] toArray(List<String> x) {
		return x.toArray(new String[x.size()]);
	}

	private <T> T newInstance(Class<T> c) {
		try {
			return c.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("header", header)
			.append("nav", nav)
			.append("navlinks", navlinks)
			.append("aside", aside)
			.append("asideFloat", asideFloat)
			.append("footer", footer)
			.append("style", style)
			.append("head", head)
			.append("stylesheet", stylesheet)
			.append("nowrap", nowrap)
			.append("template", template)
			.append("noResultsMessage", noResultsMessage)
			.append("widgets", widgets);
	}
}
