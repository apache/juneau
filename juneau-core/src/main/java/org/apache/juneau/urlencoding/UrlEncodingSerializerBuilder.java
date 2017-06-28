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
package org.apache.juneau.urlencoding;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of URL-Encoding serializers.
 */
public class UrlEncodingSerializerBuilder extends UonSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public UrlEncodingSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public UrlEncodingSerializerBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializer build() {
		return new UrlEncodingSerializer(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncoding.expandedParams"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * If <jk>true</jk>, serializing the same array results in <code>?key=1&amp;key=2&amp;key=3</code>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1 = {<js>"a"</js>,<js>"b"</js>};
	 * 		<jk>public</jk> List&lt;String&gt; f2 = <jk>new</jk> LinkedList&lt;String&gt;(Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>}));
	 * 	}
	 *
	 * 	UrlEncodingSerializer s1 = UrlEncodingSerializer.<jsf>DEFAULT</jsf>;
	 * 	UrlEncodingSerializer s2 = <jk>new</jk> UrlEncodingSerializerBuilder().expandedParams(<jk>true</jk>).build();
	 *
	 * 	String ss1 = s1.serialize(<jk>new</jk> A()); <jc>// Produces "f1=(a,b)&amp;f2=(c,d)"</jc>
	 * 	String ss2 = s2.serialize(<jk>new</jk> A()); <jc>// Produces "f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</jc>
	 * </p>
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>URLENC_expandedParams</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see UrlEncodingContext#URLENC_expandedParams
	 */
	public UrlEncodingSerializerBuilder expandedParams(boolean value) {
		return property(UrlEncodingContext.URLENC_expandedParams, value);
	}

	/**
	 * <b>Configuration property:</b>  Format to use for top-level query names and simple parameters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncodingSerializer.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UON"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"UON"</js> (default) - Use UON notation for values.
	 * 		<br>String values such as <js>"(foo='bar')"</js> will end up being quoted and escaped to <js>"'(foo=bar~'baz~')'"</js>.
	 * 		<br>Similarly, boolean and numeric values will also end up quoted.
	 * 	<li>
	 * 		<js>"PLAINTEXT"</js> (default) - Serialize as plain text.
	 * 		<br>Strings will never be quoted or escaped.
	 * 		<br>Note that this can cause errors during parsing if you're using the URL-encoding parser to parse
	 * 		the results since UON constructs won't be differentiable.
	 * 		<br>However, this is not an issue if you're simply creating queries or form posts against 3rd-party interfaces.
	 * </ul>
	 *
	 * @param paramFormat The new value for this property.
	 * @return This object (for method chaining).
	 * @see UonSerializerContext#UON_paramFormat
	 */
	public UrlEncodingSerializerBuilder paramFormat(String paramFormat) {
		return property(UonSerializerContext.UON_paramFormat, paramFormat);
	}

	/**
	 * Shortcut for calling <code>paramFormat(<js>"PLAINTEXT"</js>)</code>.
	 *
	 * @return This object (for method chaining).
	 * @see UonSerializerContext#UON_paramFormat
	 */
	public UrlEncodingSerializerBuilder plainTextParams() {
		return paramFormat("PLAINTEXT");
	}

	@Override /* UonSerializerBuilder */
	public UrlEncodingSerializerBuilder encodeChars(boolean value) {
		super.encodeChars(value);
		return this;
	}

	@Override /* UonSerializerBuilder */
	public UrlEncodingSerializerBuilder encoding() {
		super.encoding();
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UrlEncodingSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> UrlEncodingSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UrlEncodingSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}