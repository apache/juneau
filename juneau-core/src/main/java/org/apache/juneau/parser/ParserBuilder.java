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
package org.apache.juneau.parser;

import static org.apache.juneau.parser.ParserContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;

/**
 * Builder class for building instances of parsers.
 */
public class ParserBuilder extends CoreObjectBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public ParserBuilder(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObjectBuilder */
	public Parser build() {
		return null;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Trim parsed strings.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.trimStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to the POJO.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>PARSER_trimStrings</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_trimStrings
	 */
	public ParserBuilder trimStrings(boolean value) {
		return property(PARSER_trimStrings, value);
	}

	/**
	 * <b>Configuration property:</b>  Strict mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.strict"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 * <p>
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>PARSER_strict</jsf>,value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_strict
	 */
	public ParserBuilder strict(boolean value) {
		return property(PARSER_strict, value);
	}

	/**
	 * Shortcut for calling <code>strict(<jk>true</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public ParserBuilder strict() {
		return strict(true);
	}

	/**
	 * <b>Configuration property:</b>  Input stream charset.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.inputStreamCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UTF-8"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The character set to use for converting <code>InputStreams</code> and byte arrays to readers.
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>PARSER_inputStreamCharset</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_inputStreamCharset
	 */
	public ParserBuilder inputStreamCharset(String value) {
		return property(PARSER_inputStreamCharset, value);
	}

	/**
	 * <b>Configuration property:</b>  File charset.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.fileCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"default"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The character set to use for reading <code>Files</code> from the file system.
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 * <p>
	 * <js>"default"</js> can be used to indicate the JVM default file system charset.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>PARSER_fileCharset</jsf>,value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see ParserContext#PARSER_fileCharset
	 */
	public ParserBuilder fileCharset(String value) {
		return property(PARSER_fileCharset, value);
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder methodVisibility(Visibility value) {
		super.methodVisibility(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder notBeanPackages(Collection<String> values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setNotBeanPackages(String...values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setNotBeanPackages(Collection<String> values) {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeNotBeanPackages(String...values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeNotBeanPackages(Collection<String> values) {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder notBeanClasses(Collection<Class<?>> values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setNotBeanClasses(Class<?>...values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setNotBeanClasses(Collection<Class<?>> values) {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeNotBeanClasses(Class<?>...values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeNotBeanClasses(Collection<Class<?>> values) {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanFilters(Collection<Class<?>> values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setBeanFilters(Class<?>...values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setBeanFilters(Collection<Class<?>> values) {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeBeanFilters(Class<?>...values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeBeanFilters(Collection<Class<?>> values) {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder pojoSwaps(Collection<Class<?>> values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setPojoSwaps(Class<?>...values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setPojoSwaps(Collection<Class<?>> values) {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removePojoSwaps(Class<?>...values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removePojoSwaps(Collection<Class<?>> values) {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public <T> CoreObjectBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanDictionary(Collection<Class<?>> values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setBeanDictionary(Class<?>...values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder setBeanDictionary(Collection<Class<?>> values) {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeFromBeanDictionary(Class<?>...values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder defaultParser(Class<?> value) {
		super.defaultParser(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder debug(boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder property(String name, Object value) {
		super.property(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder properties(Map<String,Object> properties) {
		super.properties(properties);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder addToProperty(String name, Object value) {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder putToProperty(String name, Object key, Object value) {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder putToProperty(String name, Object value) {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder removeFromProperty(String name, Object value) {
		super.removeFromProperty(name, value);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder classLoader(ClassLoader classLoader) {
		super.classLoader(classLoader);
		return this;
	}

	@Override /* CoreObjectBuilder */
	public ParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}