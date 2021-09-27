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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of HTML Doc serializers.
 * {@review}
 */
@FluentSetters
public class HtmlDocSerializerBuilder extends HtmlStrippedDocSerializerBuilder {

	List<String> aside, footer, head, header, nav, navlinks, script, style, stylesheet;
	AsideFloat asideFloat;
	String noResultsMessage;
	boolean nowrap;
	Class<? extends HtmlDocTemplate> template;
	List<Class<? extends HtmlWidget>> widgets;

	/**
	 * Constructor, default settings.
	 */
	protected HtmlDocSerializerBuilder() {
		super();
		produces("text/html");
		accept("text/html");
		type(HtmlDocSerializer.class);
		asideFloat = AsideFloat.RIGHT;
		noResultsMessage = "<p>no results</p>";
		template = BasicHtmlDocTemplate.class;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected HtmlDocSerializerBuilder(HtmlDocSerializer copyFrom) {
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
	protected HtmlDocSerializerBuilder(HtmlDocSerializerBuilder copyFrom) {
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

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder copy() {
		return new HtmlDocSerializerBuilder(this);
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializer build() {
		return (HtmlDocSerializer)super.build();
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
	 * <p class='bcode w800'>
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format: HTML
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		A value of <js>"NONE"</js> can be used to force no value.
	 * 	<li>
	 * 		The parent value can be included by adding the literal <js>"INHERIT"</js> as a value.
	 * 	<li>
	 * 		Multiple values are combined with newlines into a single string.
	 * 	<li>
	 * 		On methods, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the servlet/resource class.
	 * 	<li>
	 * 		On servlet/resource classes, this value is inherited from the <ja>@HtmlDocConfig</ja> annotation on the
	 * 		parent class.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder aside(String...value) {
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
			aside = new ArrayList<>();
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
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder asideFloat(AsideFloat value) {
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
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder footer(String...value) {
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
			footer = new ArrayList<>();
		return footer;
	}

	/**
	 * Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder head(String...value) {
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
			head = new ArrayList<>();
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
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder header(String...value) {
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
			header = new ArrayList<>();
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
	 * <p class='bcode w800'>
	 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.nav(
	 * 			<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
	 * 		)
	 * 		.build()
	 * </p>
	 *
	 * <p>
	 * When this property is specified, the {@link HtmlDocSerializerBuilder#navlinks(String...)} property is ignored.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder nav(String...value) {
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
			nav = new ArrayList<>();
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
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder navlinks(String...value) {
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
			navlinks = new ArrayList<>();
		return navlinks;
	}

	/**
	 * No-results message.
	 *
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.noResultsMessage("&lt;b&gt;This interface is great!&lt;/b&gt;"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * A value of <js>"NONE"</js> can be used to represent no value to differentiate it from an empty string.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder noResultsMessage(String value) {
		noResultsMessage = value;
		return this;
	}

	/**
	 * Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder nowrap() {
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
	public HtmlDocSerializerBuilder nowrap(boolean value) {
		nowrap = value;
		return this;
	}

	/**
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <p>
	 * A shortcut on <ja>@Rest</ja> is also provided for this setting:
	 * <p class='bcode w800'>
	 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.script(<js>"alert('hello!');"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The value to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder script(String...value) {
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
			script = new ArrayList<>();
		return script;
	}

	/**
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <p class='bcode w800'>
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder style(String...value) {
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
			style = new ArrayList<>();
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder stylesheet(String...value) {
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
			stylesheet = new ArrayList<>();
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
	 * <p class='bcode w800'>
	 * 	WriterSerializer <jv>serializer</jv> = HtmlDocSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.template9MySpecialDocTemplate.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder template(Class<? extends HtmlDocTemplate> value) {
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
	 * <p class='bcode w800'>
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Widgets are inherited from super classes, but can be overridden by reusing the widget name.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestHtmlWidgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	@SuppressWarnings("unchecked")
	public HtmlDocSerializerBuilder widgets(Class<? extends HtmlWidget>...values) {
		Collections.addAll(widgets(), values);
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
			widgets = new ArrayList<>();
		return widgets;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlDocSerializerBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder swaps(Class<?>...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlDocSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlDocSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlDocSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlDocSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlDocSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlDocSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder addNamespaceUrisToRoot() {
		super.addNamespaceUrisToRoot();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder disableAutoDetectNamespaces() {
		super.disableAutoDetectNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder enableNamespaces() {
		super.enableNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder ns() {
		super.ns();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder addKeyValueTableHeaders() {
		super.addKeyValueTableHeaders();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder disableDetectLabelParameters() {
		super.disableDetectLabelParameters();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder disableDetectLinksInStrings() {
		super.disableDetectLinksInStrings();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder labelParameter(String value) {
		super.labelParameter(value);
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder uriAnchorText(AnchorText value) {
		super.uriAnchorText(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static <T> List<T> copy(List<T> s) {
		return s == null || s.isEmpty() ? null : new ArrayList<>(s);
	}

	private static <T> List<T> copy(T[] s) {
		return s.length == 0 ? null : new ArrayList<>(Arrays.asList(s));
	}

	private List<String> merge(List<String> old, String[] newValues) {
		List<String> x = new ArrayList<>(newValues.length);
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
		List<String> x = new ArrayList<>(newValues.length);
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