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
package org.apache.juneau.xml;

import static org.apache.juneau.xml.XmlParser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building XML parsers.
 */
public class XmlParserBuilder extends ReaderParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public XmlParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public XmlParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public XmlParser build() {
		return build(XmlParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML event allocator.
	 *
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_eventAllocator}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder eventAllocator(XMLEventAllocator value) {
		return set(XML_eventAllocator, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML event allocator.
	 *
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_eventAllocator}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder eventAllocator(Class<? extends XMLEventAllocator> value) {
		return set(XML_eventAllocator, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  Preserve root element during generalized parsing.
	 *
	 * <p>
	 * Shortcut for calling <code>preserveRootElement(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_preserveRootElement}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder preserveRootElement() {
		return set(XML_preserveRootElement, true);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML reporter.
	 *
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_reporter}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder reporter(XMLReporter value) {
		return set(XML_reporter, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML reporter.
	 *
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_reporter}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder reporter(Class<? extends XMLReporter> value) {
		return set(XML_reporter, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML resolver.
	 *
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_resolver}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder resolver(XMLResolver value) {
		return set(XML_resolver, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  XML resolver.
	 *
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_resolver}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder resolver(Class<? extends XMLResolver> value) {
		return set(XML_resolver, value);
	}

	/**
	 * <i><l>XmlParser</l> configuration property:&emsp;</i>  Enable validation.
	 *
	 * <p>
	 * Shortcut for calling <code>validating(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link XmlParser#XML_validating}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public XmlParserBuilder validating() {
		return set(XML_validating, true);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public XmlParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> XmlParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> XmlParserBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public XmlParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public XmlParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public XmlParserBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public XmlParserBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	// </FluentSetters>
}