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
package org.apache.juneau.uon;

import static org.apache.juneau.uon.UonSerializerContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Builder class for building instances of UON serializers.
 */
public class UonSerializerBuilder extends SerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public UonSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public UonSerializerBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public UonSerializer build() {
		return new UonSerializer(propertyStore);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Encode non-valid URI characters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.encodeChars"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk> for {@link UonSerializer}, <jk>true</jk> for {@link UrlEncodingSerializer}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 *
	 * <p>
	 * If <jk>true</jk>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * Set to <jk>false</jk> if parameter value is being passed to some other code that will already perform
	 * URL-encoding of non-valid URI characters.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>UON_encodeChars</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see UonSerializerContext#UON_encodeChars
	 */
	public UonSerializerBuilder encodeChars(boolean value) {
		return property(UON_encodeChars, value);
	}

	/**
	 * Shortcut for calling <code>setEncodeChars(true)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public UonSerializerBuilder encoding() {
		return encodeChars(true);
	}

	/**
	 * <b>Configuration property:</b>  Format to use for query/form-data/header values.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncodingSerializer.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>ParamFormat</code>
	 * 	<li><b>Default:</b> <jsf>UON</jsf>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>UON_paramFormat</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see UonSerializerContext#UON_paramFormat
	 */
	public UonSerializerBuilder paramFormat(ParamFormat value) {
		return property(UON_paramFormat, value);
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public UonSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> UonSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder includeProperties(String beanClassName, String properties) {
		super.includeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder includeProperties(Class<?> beanClass, String properties) {
		super.includeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder excludeProperties(String beanClassName, String properties) {
		super.excludeProperties(beanClassName, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public UonSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}