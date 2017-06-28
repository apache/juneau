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

import static org.apache.juneau.html.HtmlSerializerContext.*;

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
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public HtmlSerializerBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializer build() {
		return new HtmlSerializer(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Anchor text source.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.uriAnchorText"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"toString"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs><xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>)
	 * in HTML, this setting defines what to set the inner text to.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@link HtmlSerializerContext#TO_STRING} / <js>"toString"</js> - Set to whatever is returned by
	 * 		{@link #toString()} on the object.
	 * 	<li>
	 * 		{@link HtmlSerializerContext#URI} / <js>"uri"</js> - Set to the URI value.
	 * 	<li>
	 * 		{@link HtmlSerializerContext#LAST_TOKEN} / <js>"lastToken"</js> - Set to the last token of the URI value.
	 * 	<li>
	 * 		{@link HtmlSerializerContext#PROPERTY_NAME} / <js>"propertyName"</js> - Set to the bean property name.
	 * 	<li>
	 * 		{@link HtmlSerializerContext#URI_ANCHOR} / <js>"uriAnchor"</js> - Set to the anchor of the URL.
	 * 		(e.g. <js>"http://localhost:9080/foobar#anchorTextHere"</js>)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>HTML_uriAnchorText</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see HtmlSerializerContext#HTML_uriAnchorText
	 */
	public HtmlSerializerBuilder uriAnchorText(String value) {
		return property(HTML_uriAnchorText, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for URLs in {@link String Strings}.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.detectLinksInStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link HtmlSerializerContext#HTML_uriAnchorText}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>HTML_detectLinksInStrings</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see HtmlSerializerContext#HTML_detectLinksInStrings
	 */
	public HtmlSerializerBuilder detectLinksInStrings(boolean value) {
		return property(HTML_detectLinksInStrings, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for link labels in the <js>"label"</js> parameter of the URL.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.lookForLabelParameters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link HtmlSerializerContext#HTML_labelParameter} property.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>HTML_lookForLabelParameters</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see HtmlSerializerContext#HTML_lookForLabelParameters
	 */
	public HtmlSerializerBuilder lookForLabelParameters(boolean value) {
		return property(HTML_lookForLabelParameters, value);
	}

	/**
	 * <b>Configuration property:</b>  The parameter name to use when using {@link HtmlSerializerContext#HTML_lookForLabelParameters}.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.labelParameter"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"label"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>HTML_labelParameter</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see HtmlSerializerContext#HTML_labelParameter
	 */
	public HtmlSerializerBuilder labelParameter(String value) {
		return property(HTML_labelParameter, value);
	}

	/**
	 * <b>Configuration property:</b>  Add key/value headers on bean/map tables.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.addKeyValueTableHeaders"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>HTML_addKeyValueTableHeaders</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see HtmlSerializerContext#HTML_addKeyValueTableHeaders
	 */
	public HtmlSerializerBuilder addKeyValueTableHeaders(boolean value) {
		return property(HTML_addKeyValueTableHeaders, value);
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder enableNamespaces(boolean value) {
		super.enableNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder autoDetectNamespaces(boolean value) {
		super.autoDetectNamespaces(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder addNamespaceUrisToRoot(boolean value) {
		super.addNamespaceUrisToRoot(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder xsNamespace(Namespace value) {
		super.xsNamespace(value);
		return this;
	}

	@Override /* XmlSerializerBuilder */
	public HtmlSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
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
	public HtmlSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
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
	public HtmlSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public HtmlSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> HtmlSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public HtmlSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}