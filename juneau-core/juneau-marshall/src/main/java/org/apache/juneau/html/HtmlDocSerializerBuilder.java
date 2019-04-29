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
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
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
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public HtmlDocSerializerBuilder applyAnnotations(AnnotationsMap m, StringResolver sr) throws ParseException {
		super.applyAnnotations(m, sr);
		if (! m.containsKey(HtmlDocConfig.class))
			return this;
		ObjectResolver r = new ObjectResolver(sr);
		for (HtmlDocConfig a : m.get(HtmlDocConfig.class)) {
			if (a.aside().length > 0)
				aside(r.strings(a.aside()));
			if (a.footer().length > 0)
				footer(r.strings(a.footer()));
			if (a.head().length > 0)
				head(r.strings(a.head()));
			if (a.header().length > 0)
				header(r.strings(a.header()));
			if (a.nav().length > 0)
				nav(r.strings(a.nav()));
			if (a.navlinks().length > 0)
				navlinks(r.strings(a.navlinks()));
			if (a.navlinks_replace().length > 0)
				navlinks_replace(r.strings(a.navlinks_replace()));
			if (! a.noResultsMessage().isEmpty())
				noResultsMessage(r.string(a.noResultsMessage()));
			if (! a.nowrap().isEmpty())
				nowrap(r.bool(a.nowrap()));
			if (a.script().length > 0)
				script(r.strings(a.script()));
			if (a.script_replace().length > 0)
				script_replace(r.strings(a.script_replace()));
			if (a.style().length > 0)
				style(r.strings(a.style()));
			if (a.style_replace().length > 0)
				style_replace(r.strings(a.style_replace()));
			if (a.stylesheet().length > 0)
				stylesheet(r.strings(a.stylesheet()));
			if (a.stylesheet_replace().length > 0)
				stylesheet_replace
				(r.strings(a.stylesheet_replace()));
			if (a.template() != HtmlDocTemplate.Null.class)
				template(a.template());
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Aside section contents.
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
	 */
	private HtmlDocSerializerBuilder aside(String[] value) {
		set(HTMLDOC_aside, value);
		return this;
	}

	/**
	 * Configuration property:  Footer section contents.
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
	 */
	private HtmlDocSerializerBuilder footer(String[] value) {
		set(HTMLDOC_footer, value);
		return this;
	}

	/**
	 * Configuration property:  Additional head section content.
	 *
	 * <p>
	 * Adds the specified HTML content to the head section of the page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@HtmlDoc</ja>(
	 * 		head={
	 * 			<js>"&lt;link rel='icon' href='$U{servlet:/htdocs/mypageicon.ico}'&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 */
	private HtmlDocSerializerBuilder head(String[] value) {
		set(HTMLDOC_head, value);
		return this;
	}

	/**
	 * Configuration property:  Header section contents.
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
	 */
	private HtmlDocSerializerBuilder header(String[] value) {
		set(HTMLDOC_header, value);
		return this;
	}

	/**
	 * Configuration property:  Nav section contents.
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
	 * When this property is specified, the {@link #HTMLDOC_navlinks} property is ignored.
	 *
	 * @param value
	 * 	The new value for this property.
	 */
	private HtmlDocSerializerBuilder nav(String[] value) {
		set(HTMLDOC_nav, value);
		return this;
	}

	/**
	 * Configuration property:  Page navigation links.
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
	 * <p>
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"options: ?method=OPTIONS"</js>,
	 * 			<js>"doc: doc"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> AddressBookResource <jk>extends</jk> BasicRestServletJena {
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 */
	private HtmlDocSerializerBuilder navlinks_replace(String[] value) {
		set(HTMLDOC_navlinks, value);
		return this;
	}

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_navlinks} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 */
	private HtmlDocSerializerBuilder navlinks(String[] value) {
		set(HTMLDOC_navlinks_add, value);
		return this;
	}

	/**
	 * Configuration property:  No-results message.
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
	 */
	private HtmlDocSerializerBuilder noResultsMessage(String value) {
		set(HTMLDOC_noResultsMessage, value);
		return this;
	}

	/**
	 * Configuration property:  Prevent word wrap on page.
	 *
	 * <p>
	 * Adds <js>"* {white-space:nowrap}"</js> to the CSS instructions on the page to prevent word wrapping.
	 *
	 * @param value
	 * 	The new value for this property.
	 */
	private HtmlDocSerializerBuilder nowrap(boolean value) {
		set(HTMLDOC_nowrap, value);
		return this;
	}

	/**
	 * Configuration property:  Javascript code.
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
	 */
	private HtmlDocSerializerBuilder script_replace(String[] value) {
		set(HTMLDOC_script, value);
		return this;
	}

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_script} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 */
	private HtmlDocSerializerBuilder script(String[] value) {
		set(HTMLDOC_script_add, value);
		return this;
	}

	/**
	 * Configuration property:  CSS style code.
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
	 */
	private HtmlDocSerializerBuilder style_replace(String[] value) {
		set(HTMLDOC_style, value);
		return this;
	}

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_style} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 */
	private HtmlDocSerializerBuilder style(String[] value) {
		set(HTMLDOC_style_add, value);
		return this;
	}

	/**
	 * Configuration property:  Stylesheet import URLs.
	 *
	 * <p>
	 * Adds a link to the specified stylesheet URL.
	 *
	 * <p>
	 * Note that this stylesheet is controlled by the <code><ja>@RestResource</ja>.stylesheet()</code> annotation.
	 *
	 * @param value
	 * 	The new value for this property.
	 */
	private HtmlDocSerializerBuilder stylesheet_replace(String[] value) {
		set(HTMLDOC_stylesheet, value);
		return this;
	}

	/**
	 * Configuration property:  Add to the {@link #HTMLDOC_stylesheet} property.
	 *
	 * @param value
	 * 	The value to add to this property.
	 */
	private HtmlDocSerializerBuilder stylesheet(String[] value) {
		set(HTMLDOC_stylesheet_add, value);
		return this;
	}

	/**
	 * Configuration property:  HTML document template.
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
	 */
	private HtmlDocSerializerBuilder template(Class<?> value) {
		set(HTMLDOC_template, value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder addKeyValueTableHeaders(boolean value) {
		super.addKeyValueTableHeaders(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder addKeyValueTableHeaders() {
		super.addKeyValueTableHeaders();
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder detectLinksInStrings(boolean value) {
		super.detectLinksInStrings(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder labelParameter(String value) {
		super.labelParameter(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder detectLabelParameters(boolean value) {
		super.detectLabelParameters(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder uriAnchorText(AnchorText value) {
		super.uriAnchorText(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder uriAnchorText(String value) {
		super.uriAnchorText(value);
		return this;
	}

	@Override /* HtmlSerializerBuilder */
	public HtmlDocSerializerBuilder addNamespaceUrisToRoot(boolean value) {
		super.addNamespaceUrisToRoot(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder addNamespaceUrisToRoot() {
		super.addNamespaceUrisToRoot();
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder autoDetectNamespaces(boolean value) {
		super.autoDetectNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder enableNamespaces(boolean value) {
		super.enableNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlDocSerializerBuilder xsNamespace(Namespace value) {
		super.xsNamespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder addBeanTypes(boolean value) {
		super.addBeanTypes(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder addRootType(boolean value) {
		super.addRootType(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}
	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlDocSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionaryReplace(Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> HtmlDocSerializerBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlDocSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlDocSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}