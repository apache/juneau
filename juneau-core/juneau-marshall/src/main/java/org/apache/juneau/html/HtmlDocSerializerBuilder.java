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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of HTML Doc serializers.
 */
public class HtmlDocSerializerBuilder extends HtmlStrippedDocSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public HtmlDocSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public HtmlDocSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializer build() {
		return build(HtmlDocSerializer.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Aside section contents.
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
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		aside={
	 * 			<js>"&lt;ul&gt;"</js>,
	 * 			<js>"	&lt;li&gt;Item 1"</js>,
	 * 			<js>"	&lt;li&gt;Item 2"</js>,
	 * 			<js>"	&lt;li&gt;Item 3"</js>,
	 * 			<js>"&lt;/ul&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder aside(String[] value) {
		set(HTMLDOC_aside, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Float aside section contents.
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
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlDocSerializerBuilder#asideFloat(Float)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to position the aside contents of the page around the main contents.
	 *
	 * <p>
	 * By default, the aside section is floated to the right.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *  <ja>@HtmlDocConfig</ja>(
	 * 		aside={
	 * 			<js>"&lt;ul&gt;"</js>,
	 * 			<js>"	&lt;li&gt;Item 1"</js>,
	 * 			<js>"	&lt;li&gt;Item 2"</js>,
	 * 			<js>"	&lt;li&gt;Item 3"</js>,
	 * 			<js>"&lt;/ul&gt;"</js>
	 * 		},
	 * 		asideFloat=<js>"RIGHT"</js>
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder asideFloat(Float value) {
		set(HTMLDOC_asideFloat, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Footer section contents.
	 *
	 * <p>
	 * Allows you to specify the contents of the footer section on the HTML page.
	 *
	 * <p>
	 * By default, the footer section is empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		footer={
	 * 			<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder footer(String[] value) {
		set(HTMLDOC_footer, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		head={
	 * 			<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder head(String[] value) {
		set(HTMLDOC_head, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Header section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the header section on the HTML page.
	 * The header section normally contains the title and description at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;My own header&lt;/h1&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder header(String[] value) {
		set(HTMLDOC_header, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Nav section contents.
	 *
	 * <p>
	 * Allows you to override the contents of the nav section on the HTML page.
	 * The nav section normally contains the page links at the top of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		nav={
	 * 			<js>"&lt;p class='special-navigation'&gt;This is my special navigation content&lt;/p&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * When this property is specified, the {@link HtmlDocSerializer#HTMLDOC_navlinks} property is ignored.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder nav(String[] value) {
		set(HTMLDOC_nav, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Page navigation links.
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
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"options: servlet:/?method=OPTIONS"</js>,
	 * 			<js>"stats: servlet:/stats"</js>,
	 * 			<js>"doc: doc"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder navlinks_replace(String[] value) {
		set(HTMLDOC_navlinks, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Add to the {@link HtmlDocSerializer#HTMLDOC_navlinks} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder navlinks(String[] value) {
		set(HTMLDOC_navlinks_add, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  No-results message.
	 *
	 * <p>
	 * Allows you to specify the string message used when trying to serialize an empty array or empty list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		noResultsMessage=<js>"&lt;b&gt;This interface is great!&lt;/b&gt;"</js>
	 * 	)
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
		set(HTMLDOC_noResultsMessage, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default ios <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder nowrap(boolean value) {
		set(HTMLDOC_nowrap, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder nowrap() {
		set(HTMLDOC_nowrap, true);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Javascript code.
	 *
	 * <p>
	 * Adds the specified Javascript code to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		script={
	 * 			<js>"alert('hello!');"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder script_replace(String[] value) {
		set(HTMLDOC_script, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Add to the {@link HtmlDocSerializer#HTMLDOC_script} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder script(String[] value) {
		set(HTMLDOC_script_add, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  CSS style code.
	 *
	 * <p>
	 * Adds the specified CSS instructions to the HTML page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		style={
	 * 			<js>"h3 { color: red; }"</js>,
	 * 			<js>"h5 { font-weight: bold; }"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder style_replace(String[] value) {
		set(HTMLDOC_style, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Add to the {@link HtmlDocSerializer#HTMLDOC_style} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder style(String[] value) {
		set(HTMLDOC_style_add, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Stylesheet import URLs.
	 *
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <p>
	 * Note that this stylesheet is controlled by the <code><ja>@Rest</ja>.stylesheet()</code> annotation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder stylesheet_replace(String[] value) {
		set(HTMLDOC_stylesheet, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  Add to the {@link HtmlDocSerializer#HTMLDOC_stylesheet} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder stylesheet(String[] value) {
		set(HTMLDOC_stylesheet_add, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  HTML document template.
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
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		template=MySpecialDocTemplate.<jk>class</jk>
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder template(Class<?> value) {
		set(HTMLDOC_template, value);
		return this;
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  HTML Widgets.
	 *
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	@SuppressWarnings("unchecked")
	public HtmlDocSerializerBuilder widgets(Class<? extends HtmlWidget>...values) {
		return prependTo(HTMLDOC_widgets, values);
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  HTML Widgets.
	 *
	 * <p>
	 * Same as {@link #widgets(Class...)} but replaces any previous values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_widgets}
	 * </ul>
	 *
	 * @param values The values to set on this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	@SuppressWarnings("unchecked")
	public HtmlDocSerializerBuilder widgetsReplace(Class<? extends HtmlWidget>...values) {
		return set(HTMLDOC_widgets, values);
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  HTML Widgets.
	 *
	 * <p>
	 * Same as {@link #widgets(Class...)} except input is pre-constructed instances.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder widgets(HtmlWidget...values) {
		return prependTo(HTMLDOC_widgets, values);
	}

	/**
	 * <i><l>HtmlDocSerializer</l> configuration property:</i>  HTML Widgets.
	 *
	 * <p>
	 * Same as {@link #widgets(HtmlWidget...)} except allows you to overwrite the previous value.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link HtmlDocSerializer#HTMLDOC_widgets}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlDocSerializerBuilder widgetsReplace(HtmlWidget...values) {
		return set(HTMLDOC_widgets, values);
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
	public HtmlDocSerializerBuilder apply(PropertyStore copyFrom) {
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
	public HtmlDocSerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
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
	public HtmlDocSerializerBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
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
	public HtmlDocSerializerBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
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
	public HtmlDocSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackages(Object...values) {
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
	public HtmlDocSerializerBuilder swaps(Object...values) {
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
	public HtmlDocSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder dontAutoDetectNamespaces() {
		super.dontAutoDetectNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder enableNamespaces() {
		super.enableNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlDocSerializerBuilder namespaces(String...values) {
		super.namespaces(values);
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
	public HtmlDocSerializerBuilder dontDetectLabelParameters() {
		super.dontDetectLabelParameters();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder dontDetectLinksInStrings() {
		super.dontDetectLinksInStrings();
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
}