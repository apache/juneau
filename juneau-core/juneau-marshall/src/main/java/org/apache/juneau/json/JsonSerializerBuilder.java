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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonSerializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Builder class for building instances of JSON serializers.
 * {@review}
 */
@FluentSetters
public class JsonSerializerBuilder extends WriterSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	protected JsonSerializerBuilder() {
		super();
		produces("application/json");
		accept("application/json,text/json");
		contextClass(JsonSerializer.class);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected JsonSerializerBuilder(JsonSerializer copyFrom) {
		super(copyFrom);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected JsonSerializerBuilder(JsonSerializerBuilder copyFrom) {
		super(copyFrom);
	}

	@Override /* ContextBuilder */
	public JsonSerializerBuilder copy() {
		return new JsonSerializerBuilder(this);
	}

	@Override /* ContextBuilder */
	public JsonSerializer build() {
		return (JsonSerializer)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------


	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * <p>
	 * If enabled, solidus (e.g. slash) characters should be escaped.
	 *
	 * <p>
	 * The JSON specification allows for either format.
	 * <br>However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
	 * <xt>&lt;\/script&gt;</xt>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON serializer that escapes solidus characters.</jc>
	 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.simple()
	 * 		.escapeSolidus()
	 * 		.build();
	 *
	 * 	<jc>// Produces: "{foo:'&lt;\/bar&gt;'"</jc>
	 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(OMap.<jsm>of</jsm>(<js>"foo"</js>, <js>"&lt;/bar&gt;"</js>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSerializer#JSON_escapeSolidus}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSerializerBuilder escapeSolidus() {
		return set(JSON_escapeSolidus);
	}

	/**
	 * Simple JSON mode.
	 *
	 * <p>
	 * If enabled, JSON attribute names will only be quoted when necessary.
	 * <br>Otherwise, they are always quoted.
	 *
	 * <p>
	 * Attributes do not need to be quoted when they conform to the following:
	 * <ol class='spaced-list'>
	 * 	<li>They start with an ASCII character or <js>'_'</js>.
	 * 	<li>They contain only ASCII characters or numbers or <js>'_'</js>.
	 * 	<li>They are not one of the following reserved words:
	 * 		<p class='bcode w800'>
	 * 	arguments, break, case, catch, class, const, continue, debugger, default,
	 * 	delete, do, else, enum, eval, export, extends, false, finally, for, function,
	 * 	if, implements, import, in, instanceof, interface, let, new, null, package,
	 * 	private, protected, public, return, static, super, switch, this, throw,
	 * 	true, try, typeof, var, void, while, with, undefined, yield
	 * 		</p>
	 * </ol>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON serializer in normal mode.</jc>
	 * 	WriterSerializer <jv>serializer1<jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.build();
	 *
	 * 	<jc>// Create a JSON serializer in simple mode.</jc>
	 * 	WriterSerializer <jv>serializer2<jv> = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.simple()
	 * 		.build();
	 *
	 * 	OMap <jv>myMap<jv> = OMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"x1"</js>,
	 * 		<js>"_bar"</js>, <js>"x2"</js>,
	 * 		<js>" baz "</js>, <js>"x3"</js>,
	 * 		<js>"123"</js>, <js>"x4"</js>,
	 * 		<js>"return"</js>, <js>"x5"</js>,
	 * 		<js>""</js>, <js>"x6"</js>
	 *  );
	 *
	 * 	<jc>// Produces:</jc>
	 * 	<jc>// {</jc>
	 * 	<jc>// 	"foo": "x1"</jc>
	 * 	<jc>// 	"_bar": "x2"</jc>
	 * 	<jc>// 	" baz ": "x3"</jc>
	 * 	<jc>// 	"123": "x4"</jc>
	 * 	<jc>// 	"return": "x5"</jc>
	 * 	<jc>// 	"": "x6"</jc>
	 * 	<jc>// }</jc>
	 * 	String <jv>json1<jv> = <jv>serializer1<jv>.serialize(<jv>myMap<jv>);
	 *
	 * 	<jc>// Produces:</jc>
	 * 	<jc>// {</jc>
	 * 	<jc>// 	foo: "x1"</jc>
	 * 	<jc>// 	_bar: "x2"</jc>
	 * 	<jc>// 	" baz ": "x3"</jc>
	 * 	<jc>// 	"123": "x4"</jc>
	 * 	<jc>// 	"return": "x5"</jc>
	 * 	<jc>// 	"": "x6"</jc>
	 * 	<jc>// }</jc>
	 * 	String <jv>json2<jv> = <jv>serializer2<jv>.serialize(<jv>myMap<jv>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSerializer#JSON_simpleMode}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSerializerBuilder simpleMode() {
		return set(JSON_simpleMode);
	}

	/**
	 * Simple JSON mode and single quote.
	 *
	 * <p>
	 * Shortcut for calling <c>simple().sq()</c>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSerializer#JSON_simpleMode}
	 * 	<li class='jf'>{@link JsonSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSerializerBuilder ssq() {
		return simpleMode().sq();
	}

	// <FluentSetters>

	@Override
	public JsonSerializerBuilder beanContextBuilder(BeanContextBuilder value) {
		super.beanContextBuilder(value);
		return this;
	}

	@Override
	public JsonSerializerBuilder produces(String value) {
		super.produces(value);
		return this;
	}

	@Override
	public JsonSerializerBuilder accept(String value) {
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder apply(List<AnnotationWork> work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSerializerBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSerializerBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSerializerBuilder ws() {
		super.ws();
		return this;
	}

	// </FluentSetters>
}