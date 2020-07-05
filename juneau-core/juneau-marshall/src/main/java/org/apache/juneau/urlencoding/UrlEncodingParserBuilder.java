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
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of URL-Encoding parsers.
 */
public class UrlEncodingParserBuilder extends UonParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public UrlEncodingParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public UrlEncodingParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public UrlEncodingParser build() {
		return build(UrlEncodingParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>UrlEncodingParser</l> configuration property:&emsp;</i>  Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #expandedParams()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@FluentSetter
	public UrlEncodingParserBuilder expandedParams(boolean value) {
		return set(URLENC_expandedParams, value);
	}

	/**
	 * <i><l>UrlEncodingParser</l> configuration property:&emsp;</i>  Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <p>
	 * Shortcut for calling <code>expandedParams(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UrlEncodingParser#URLENC_expandedParams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public UrlEncodingParserBuilder expandedParams() {
		return set(URLENC_expandedParams, true);
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
	public UrlEncodingParserBuilder apply(PropertyStore copyFrom) {
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
	public UrlEncodingParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
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
	public UrlEncodingParserBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
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
	public UrlEncodingParserBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
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
	public UrlEncodingParserBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
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
	public UrlEncodingParserBuilder decoding(boolean value) {
		super.decoding(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder validateEnd() {
		super.validateEnd();
		return this;
	}

	// </FluentSetters>
}