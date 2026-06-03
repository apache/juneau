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
package org.apache.juneau.html;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.svl.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class HtmlDocSerializer extends HtmlStrippedDocSerializer {

	// Property name constants
	private static final String PROP_aside = "aside";
	private static final String PROP_asideFloat = "asideFloat";
	private static final String PROP_footer = "footer";
	private static final String PROP_head = "head";
	private static final String PROP_header = "header";
	private static final String PROP_nav = "nav";
	private static final String PROP_navlinks = "navlinks";
	private static final String PROP_noResultsMessage = "noResultsMessage";
	private static final String PROP_nowrap = "nowrap";
	private static final String PROP_style = "style";
	private static final String PROP_stylesheet = "stylesheet";
	private static final String PROP_template = "template";
	private static final String PROP_widgets = "widgets";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends HtmlStrippedDocSerializer.Builder<SELF> {

		private static final Cache<HashKey,HtmlDocSerializer> CACHE = Cache.of(HashKey.class, HtmlDocSerializer.class).build();

		private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");

		private static <T> List<T> copy(List<T> s) {
			return s == null || s.isEmpty() ? null : copyOf(s);
		}

		private static <T> List<T> copy(T[] s) {
			return s.length == 0 ? null : l(s);
		}

		List<String> aside;
	List<String> footer;
	List<String> head;
	List<String> header;
	List<String> nav;
	List<String> navlinks;
	List<String> script;
	List<String> style;
	List<String> stylesheet;
		AsideFloat asideFloat;
		String noResultsMessage;

		boolean nowrap;
		boolean resolveBodyVars;

		Class<? extends HtmlDocTemplate> template;

		List<Class<? extends HtmlWidget>> widgets;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/html");
			accept("text/html");
			asideFloat = AsideFloat.RIGHT;
			noResultsMessage = "<p>no results</p>";
			template = BasicHtmlDocTemplate.class;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(copyFrom);
			aside = copy(copyFrom.aside);
			asideFloat = copyFrom.asideFloat;
			footer = copy(copyFrom.footer);
			head = copy(copyFrom.head);
			header = copy(copyFrom.header);
			nav = copy(copyFrom.nav);
			navlinks = copy(copyFrom.navlinks);
			noResultsMessage = copyFrom.noResultsMessage;
			nowrap = copyFrom.nowrap;
			resolveBodyVars = copyFrom.resolveBodyVars;
			script = copy(copyFrom.script);
			style = copy(copyFrom.style);
			stylesheet = copy(copyFrom.stylesheet);
			template = copyFrom.template;
			widgets = copy(copyFrom.widgets);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(HtmlDocSerializer copyFrom) {
			super(copyFrom);
			aside = copy(copyFrom.aside);
			asideFloat = copyFrom.asideFloat;
			footer = copy(copyFrom.footer);
			head = copy(copyFrom.head);
			header = copy(copyFrom.header);
			nav = copy(copyFrom.nav);
			navlinks = copy(copyFrom.navlinks);
			noResultsMessage = copyFrom.noResultsMessage;
			nowrap = copyFrom.nowrap;
			resolveBodyVars = copyFrom.resolveBodyVars;
			script = copy(copyFrom.script);
			style = copy(copyFrom.style);
			stylesheet = copy(copyFrom.stylesheet);
			template = copyFrom.template;
			widgets = copy(copyFrom.widgets);
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
		 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerSvlVariables">SVL Variables</a>
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
		public SELF aside(String...value) {
			aside = merge(aside, value);
			return self();
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
		public SELF asideFloat(AsideFloat value) {
			asideFloat = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HtmlDocSerializer build() {
			return cache(CACHE).build(HtmlDocSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

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
		public SELF footer(String...value) {
			footer = merge(footer, value);
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
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
				resolveBodyVars,
				template,
				widgets
			);
			// @formatter:on
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
		public SELF head(String...value) {
			head = merge(head, value);
			return self();
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
		public SELF header(String...value) {
			header = merge(header, value);
			return self();
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
		public SELF nav(String...value) {
			nav = merge(nav, value);
			return self();
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
		public SELF navlinks(String...value) {
			navlinks = mergeNavLinks(navlinks, value);
			return self();
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
		public SELF noResultsMessage(String value) {
			noResultsMessage = value;
			return self();
		}

		/**
		 * Prevent word wrap on page.
		 *
		 * <p>
		 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
		 *
		 * @return This object.
		 */
		public SELF nowrap() {
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
		public SELF nowrap(boolean value) {
			nowrap = value;
			return self();
		}

		/**
		 * Resolve $ variables in serialized POJO.
		 *
		 * @return This object.
		 */
		public SELF resolveBodyVars() {
			return resolveBodyVars(true);
		}

		/**
		 * Same as {@link #resolveBodyVars()} but allows you to explicitly specify the boolean value.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 * @see #nowrap()
		 */
		public SELF resolveBodyVars(boolean value) {
			resolveBodyVars = value;
			return self();
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
		public SELF script(String...value) {
			script = merge(script, value);
			return self();
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
		public SELF style(String...value) {
			style = merge(style, value);
			return self();
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
		 * Adds to the list of stylesheet URLs.
		 *
		 * <p>
		 * Note that this stylesheet is controlled by the <code><ja>@Rest</ja>.stylesheet()</code> annotation.
		 *
		 * @param value
		 * 	The value to add to this property.
		 * @return This object.
		 */
		public SELF stylesheet(String...value) {
			stylesheet = merge(stylesheet, value);
			return self();
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
		public SELF template(Class<? extends HtmlDocTemplate> value) {
			template = value;
			return self();
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
		 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlWidgets">Widgets</a>
		 * </ul>
		 *
		 * @param values The values to add to this setting.
		 * @return This object.
		 */
		@SuppressWarnings({
			"unchecked" // Varargs method requires unchecked cast
		})
		public SELF widgets(Class<? extends HtmlWidget>...values) {
			addAll(widgets(), values);
			return self();
		}

		private static List<String> merge(List<String> old, String[] newValues) {
			List<String> x = listOfSize(newValues.length);
			for (var s : newValues) {
				if ("NONE".equals(s)) {
					if (nn(old))
						old.clear();
				} else if ("INHERIT".equals(s)) {
					if (nn(old))
						x.addAll(old);
				} else {
					x.add(s);
				}
			}
			return x;
		}

		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for nav link merging with various input formats
		})
		private static List<String> mergeNavLinks(List<String> old, String[] newValues) {
			List<String> x = listOfSize(newValues.length);
			for (var s : newValues) {
				if ("NONE".equals(s)) {
					if (nn(old))
						old.clear();
				} else if ("INHERIT".equals(s)) {
					if (nn(old))
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
	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link HtmlDocSerializer#create()} / {@link HtmlDocSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(HtmlDocSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	private static final String[] EMPTY_ARRAY = {};

	/** Default serializer, all default settings. */
	public static final HtmlDocSerializer DEFAULT = new HtmlDocSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

final String[] style;
final String[] stylesheet;
final String[] script;
final String[] navlinks;
final String[] head;
final String[] header;
final String[] nav;
final String[] aside;
final String[] footer;
final AsideFloat asideFloat;
	final String noResultsMessage;
	final boolean nowrap;
	final boolean resolveBodyVars;
	final Class<? extends HtmlDocTemplate> template;
	final List<Class<? extends HtmlWidget>> widgets;

	private final HtmlWidgetMap widgetMap;
	private final HtmlWidget[] widgetArray;
	private final HtmlDocTemplate templateBean;

	private final AtomicReference<HtmlSchemaDocSerializer> schemaSerializer = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public HtmlDocSerializer(Builder<?> builder) {
		super(builder);
		aside = nn(builder.aside) ? toArray(builder.aside) : EMPTY_ARRAY;
		asideFloat = builder.asideFloat;
		footer = nn(builder.footer) ? toArray(builder.footer) : EMPTY_ARRAY;
		head = nn(builder.head) ? toArray(builder.head) : EMPTY_ARRAY;
		header = nn(builder.header) ? toArray(builder.header) : EMPTY_ARRAY;
		nav = nn(builder.nav) ? toArray(builder.nav) : EMPTY_ARRAY;
		navlinks = nn(builder.navlinks) ? toArray(builder.navlinks) : EMPTY_ARRAY;
		noResultsMessage = builder.noResultsMessage;
		nowrap = builder.nowrap;
		resolveBodyVars = builder.resolveBodyVars;
		script = nn(builder.script) ? toArray(builder.script) : EMPTY_ARRAY;
		style = nn(builder.style) ? toArray(builder.style) : EMPTY_ARRAY;
		stylesheet = nn(builder.stylesheet) ? toArray(builder.stylesheet) : EMPTY_ARRAY;
		template = builder.template;
		widgets = builder.widgets == null ? Collections.emptyList() : copyOf(builder.widgets);

		templateBean = newInstance(template);
		widgetMap = new HtmlWidgetMap();
		widgets.stream().map(this::newInstance).forEach(widgetMap::append);
		widgetArray = array(widgetMap.values(), HtmlWidget.class);
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public HtmlDocSerializerSession.Builder<?> createSession() {
		return HtmlDocSerializerSession.create(this);
	}

	@Override /* Overridden from XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		HtmlSchemaDocSerializer result = schemaSerializer.get();
		if (result == null) {
			result = HtmlSchemaDocSerializer.create().marshallingContext(getMarshallingContext()).build();
			if (! schemaSerializer.compareAndSet(null, result)) {
				result = schemaSerializer.get();
			}
		}
		return result;
	}

	@Override /* Overridden from Context */
	public HtmlDocSerializerSession getSession() { return createSession().build(); }

	<T> T newInstance(Class<T> c) {
		try {
			return c.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	private static String[] toArray(List<String> x) {
		return x.toArray(new String[x.size()]);
	}

	/**
	 * Performs an action on all widgets defined on this serializer.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	protected final HtmlDocSerializer forEachWidget(Consumer<HtmlWidget> action) {
		for (var w : widgetArray)
			action.accept(w);
		return this;
	}

	/**
	 * Aside section contents.
	 *
	 * @see Builder#aside(String...)
	 * @return
	 * 	The overridden contents of the aside section on the HTML page.
	 */
	protected final String[] getAside() { return aside; }

	/**
	 * Float side section contents.
	 *
	 * @see Builder#asideFloat(AsideFloat)
	 * @return
	 * 	How to float the aside contents on the page.
	 */
	protected final AsideFloat getAsideFloat() { return asideFloat; }

	/**
	 * Footer section contents.
	 *
	 * @see Builder#footer(String...)
	 * @return
	 * 	The overridden contents of the footer section on the HTML page.
	 */
	protected final String[] getFooter() { return footer; }

	/**
	 * Additional head section content.
	 *
	 * @see Builder#head(String...)
	 * @return
	 * 	HTML content to add to the head section of the HTML page.
	 */
	protected final String[] getHead() { return head; }

	/**
	 * Header section contents.
	 *
	 * @see Builder#header(String...)
	 * @return
	 * 	The overridden contents of the header section on the HTML page.
	 */
	protected final String[] getHeader() { return header; }

	/**
	 * Nav section contents.
	 *
	 * @see Builder#nav(String...)
	 * @return
	 * 	The overridden contents of the nav section on the HTML page.
	 */
	protected final String[] getNav() { return nav; }

	/**
	 * Page navigation links.
	 *
	 * @see Builder#navlinks(String...)
	 * @return
	 * 	Navigation links to add to the HTML page.
	 */
	protected final String[] getNavlinks() { return navlinks; }

	/**
	 * No-results message.
	 *
	 * @see Builder#noResultsMessage(String)
	 * @return
	 * 	The message used when serializing an empty array or empty list.
	 */
	protected final String getNoResultsMessage() { return noResultsMessage; }

	/**
	 * Javascript code.
	 *
	 * @see Builder#script(String...)
	 * @return
	 * 	Arbitrary Javascript to add to the HTML page.
	 */
	protected final String[] getScript() { return script; }

	/**
	 * CSS style code.
	 *
	 * @see Builder#style(String...)
	 * @return
	 * 	The CSS instructions to add to the HTML page.
	 */
	protected final String[] getStyle() { return style; }

	/**
	 * Stylesheet import URLs.
	 *
	 * @see Builder#stylesheet(String...)
	 * @return
	 * 	The link to the stylesheet of the HTML page.
	 */
	protected final String[] getStylesheet() { return stylesheet; }

	/**
	 * HTML document template.
	 *
	 * @see Builder#template(Class)
	 * @return
	 * 	The template to use for serializing the page.
	 */
	protected final HtmlDocTemplate getTemplate() { return templateBean; }

	/**
	 * HTML widgets.
	 *
	 * @see Builder#widgets(Class...)
	 * @return
	 * 	Widgets defined on this serializers.
	 */
	protected final HtmlWidgetMap getWidgets() { return widgetMap; }

	/**
	 * Prevent word wrap on page.
	 *
	 * @see Builder#nowrap()
	 * @return
	 * 	<jk>true</jk> if <js>"* {white-space:nowrap}"</js> shoudl be added to the CSS instructions on the page to prevent word wrapping.
	 */
	protected final boolean isNowrap() { return nowrap; }

	@Override /* Overridden from HtmlSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_aside, aside)
			.a(PROP_asideFloat, asideFloat)
			.a(PROP_footer, footer)
			.a(PROP_head, head)
			.a(PROP_header, header)
			.a(PROP_nav, nav)
			.a(PROP_navlinks, navlinks)
			.a(PROP_noResultsMessage, noResultsMessage)
			.a(PROP_nowrap, nowrap)
			.a(PROP_style, style)
			.a(PROP_stylesheet, stylesheet)
			.a(PROP_template, template)
			.a(PROP_widgets, widgets);
	}
}