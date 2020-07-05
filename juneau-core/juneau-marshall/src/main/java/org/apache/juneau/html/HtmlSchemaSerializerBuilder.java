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

import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of HTML Schema serializers.
 */
public class HtmlSchemaSerializerBuilder extends HtmlSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public HtmlSchemaSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public HtmlSchemaSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public HtmlSchemaSerializer build() {
		return build(HtmlSchemaSerializer.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add descriptions.
	 *
	 * <p>
	 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
	 * <p>
	 * The description is the result of calling {@link ClassMeta#getFullName()}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_addDescriptionsTo}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is an empty string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder addDescriptionsTo(String value) {
		return set(JSONSCHEMA_addDescriptionsTo, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add examples.
	 *
	 * <p>
	 * Identifies which categories of types that examples should be automatically added to generated schemas.
	 * <p>
	 * The examples come from calling {@link ClassMeta#getExample(BeanSession)} which in turn gets examples
	 * from the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_addExamplesTo}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is an empty string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder addExamplesTo(String value) {
		return set(JSONSCHEMA_addExamplesTo, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #allowNestedDescriptions()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public HtmlSchemaSerializerBuilder allowNestedDescriptions(boolean value) {
		return set(JSONSCHEMA_allowNestedDescriptions, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
	 *
	 * <p>
	 * Identifies whether nested descriptions are allowed in schema definitions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_allowNestedDescriptions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder allowNestedDescriptions() {
		return set(JSONSCHEMA_allowNestedDescriptions, true);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #allowNestedExamples()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public HtmlSchemaSerializerBuilder allowNestedExamples(boolean value) {
		return set(JSONSCHEMA_allowNestedExamples, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
	 *
	 * <p>
	 * Identifies whether nested examples are allowed in schema definitions.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_allowNestedExamples}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder allowNestedExamples() {
		return set(JSONSCHEMA_allowNestedExamples, true);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
	 *
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 * <p>
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 * <p>
	 * This setting is ignored if {@link JsonSchemaGenerator#JSONSCHEMA_useBeanDefs} is not enabled.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_beanDefMapper}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder beanDefMapper(Class<? extends BeanDefMapper> value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Bean schema definition mapper.
	 *
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 * <p>
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 * <p>
	 * This setting is ignored if {@link JsonSchemaGenerator#JSONSCHEMA_useBeanDefs} is not enabled.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_beanDefMapper}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder beanDefMapper(BeanDefMapper value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Default schemas.
	 *
	 * <p>
	 * Allows you to override or provide custom schema information for particular class types.
	 * <p>
	 * Keys are full class names.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonSchemaGenerator#JSONSCHEMA_defaultSchemas}
	 * </ul>
	 *
	 * @param c
	 * 	The class to define a default schema for.
	 * @param schema
	 * 	The schema.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder defaultSchema(Class<?> c, OMap schema) {
		return putTo(JSONSCHEMA_defaultSchemas, c.getName(), schema);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #useBeanDefs()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public HtmlSchemaSerializerBuilder useBeanDefs(boolean value) {
		return set(JSONSCHEMA_useBeanDefs, value);
	}

	/**
	 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
	 *
	 * <p>
	 * When enabled, schemas on beans will be serialized as the following:
	 * <p class='bcode w800'>
	 * 	{
	 * 		type: <js>'object'</js>,
	 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
	 * 	}
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSchemaSerializerBuilder useBeanDefs() {
		return set(JSONSCHEMA_useBeanDefs, true);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSchemaSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlSchemaSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlSchemaSerializerBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSchemaSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSchemaSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSchemaSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSchemaSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSchemaSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSchemaSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSchemaSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder addNamespaceUrisToRoot() {
		super.addNamespaceUrisToRoot();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder dontAutoDetectNamespaces() {
		super.dontAutoDetectNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder enableNamespaces() {
		super.enableNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder namespaces(String...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder ns() {
		super.ns();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder addKeyValueTableHeaders() {
		super.addKeyValueTableHeaders();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder dontDetectLabelParameters() {
		super.dontDetectLabelParameters();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder dontDetectLinksInStrings() {
		super.dontDetectLinksInStrings();
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder labelParameter(String value) {
		super.labelParameter(value);
		return this;
	}

	@Override /* GENERATED - HtmlSerializerBuilder */
	public HtmlSchemaSerializerBuilder uriAnchorText(AnchorText value) {
		super.uriAnchorText(value);
		return this;
	}

	// </FluentSetters>
}