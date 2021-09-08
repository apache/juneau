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

import static org.apache.juneau.urlencoding.UrlEncodingParser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of URL-Encoding parsers.
 * {@review}
 */
@FluentSetters
public class UrlEncodingParserBuilder extends UonParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	protected UrlEncodingParserBuilder() {
		super();
		decoding();
		consumes("application/x-www-form-urlencoded");
		contextClass(UrlEncodingParser.class);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected UrlEncodingParserBuilder(UrlEncodingParser copyFrom) {
		super(copyFrom);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected UrlEncodingParserBuilder(UrlEncodingParserBuilder copyFrom) {
		super(copyFrom);
	}

	@Override /* ContextBuilder */
	public UrlEncodingParserBuilder copy() {
		return new UrlEncodingParserBuilder(this);
	}

	@Override /* ContextBuilder */
	public UrlEncodingParser build() {
		return (UrlEncodingParser)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <p>
	 * This is the parser-side equivalent of the {@link #URLENC_expandedParams} setting.
	 *
	 * <p>
	 * If <jk>false</jk>, serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br>If <jk>true</jk>, serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String[] f1;
	 * 		<jk>public</jk> List&lt;String&gt; f2;
	 * 	}
	 *
	 * 	UrlEncodingParser <jv>parser1</jv> = UrlEncodingParser.<jsf>DEFAULT</jsf>;
	 * 	UrlEncodingParser <jv>parser2</jv> = UrlEncodingParser.<jsm>create</jsm>().expandedParams().build();
	 *
	 * 	MyBean <jv>myBean1</jv> = <jv>parser1</jv>.parse(<js>"f1=@(a,b)&amp;f2=@(c,d)"</js>, A.<jk>class</jk>);
	 *
	 * 	MyBean <jv>myBean2</jv> = <jv>parser2</jv>.parse(<js>"f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</js>, A.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UrlEncodingParser#URLENC_expandedParams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public UrlEncodingParserBuilder expandedParams() {
		return set(URLENC_expandedParams);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder apply(List<AnnotationWork> work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public UrlEncodingParserBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public UrlEncodingParserBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder decoding() {
		super.decoding();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder validateEnd() {
		super.validateEnd();
		return this;
	}

	// </FluentSetters>
}