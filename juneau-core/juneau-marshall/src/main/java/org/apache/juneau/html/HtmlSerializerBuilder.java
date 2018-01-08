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

import static org.apache.juneau.html.HtmlSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of HTML serializers.
 */
public class HtmlSerializerBuilder extends XmlSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public HtmlSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public HtmlSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public HtmlSerializer build() {
		return build(HtmlSerializer.class);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add key/value headers on bean/map tables.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_addKeyValueTableHeaders}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlSerializerBuilder addKeyValueTableHeaders(boolean value) {
		return set(HTML_addKeyValueTableHeaders, value);
	}

	/**
	 * Configuration property:  Look for URLs in {@link String Strings}.
	 *
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link HtmlSerializer#HTML_uriAnchorText}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_detectLinksInStrings}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlSerializerBuilder detectLinksInStrings(boolean value) {
		return set(HTML_detectLinksInStrings, value);
	}

	/**
	 * Configuration property:  The parameter name to use when using {@link HtmlSerializer#HTML_lookForLabelParameters}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_labelParameter}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlSerializerBuilder labelParameter(String value) {
		return set(HTML_labelParameter, value);
	}

	/**
	 * Configuration property:  Look for link labels in the <js>"label"</js> parameter of the URL.
	 *
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link HtmlSerializer#HTML_labelParameter} property.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_lookForLabelParameters}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlSerializerBuilder lookForLabelParameters(boolean value) {
		return set(HTML_lookForLabelParameters, value);
	}

	/**
	 * Configuration property:  Anchor text source.
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs><xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>)
	 * in HTML, this setting defines what to set the inner text to.
	 *
	 * <p>
	 * See the {@link AnchorText} enum for possible values.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_uriAnchorText}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HtmlSerializerBuilder uriAnchorText(AnchorText value) {
		return set(HTML_uriAnchorText, value);
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder addNamespaceUrisToRoot(boolean value) {
		super.addNamespaceUrisToRoot(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder autoDetectNamespaces(boolean value) {
		super.autoDetectNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder enableNamespaces(boolean value) {
		super.enableNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder xsNamespace(Namespace value) {
		super.xsNamespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> HtmlSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public HtmlSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}