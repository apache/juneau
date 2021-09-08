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
package org.apache.juneau.jso;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Builder class for building instances of Java Serialized Object parsers.
 */
@FluentSetters
public class JsoParserBuilder extends InputStreamParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	protected JsoParserBuilder() {
		super();
		consumes("application/x-java-serialized-object");
		contextClass(JsoParser.class);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected JsoParserBuilder(JsoParser copyFrom) {
		super(copyFrom);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected JsoParserBuilder(JsoParserBuilder copyFrom) {
		super(copyFrom);
	}

	@Override /* ContextBuilder */
	public JsoParserBuilder copy() {
		return new JsoParserBuilder(this);
	}

	@Override /* ContextBuilder */
	public JsoParser build() {
		return (JsoParser)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder apply(List<AnnotationWork> work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsoParserBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsoParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsoParserBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsoParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsoParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - InputStreamParserBuilder */
	public JsoParserBuilder binaryFormat(BinaryFormat value) {
		super.binaryFormat(value);
		return this;
	}

	// </FluentSetters>
}