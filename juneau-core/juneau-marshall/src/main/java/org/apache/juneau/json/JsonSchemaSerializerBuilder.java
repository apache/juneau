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

import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for building instances of JSON Schema serializers.
 */
@FluentSetters
public class JsonSchemaSerializerBuilder extends JsonSerializerBuilder {

	JsonSchemaGeneratorBuilder generatorBuilder;

	/**
	 * Constructor, default settings.
	 */
	protected JsonSchemaSerializerBuilder() {
		super();
		produces("application/json");
		accept("application/json+schema,text/json+schema");
		contextClass(JsonSchemaSerializer.class);
		generatorBuilder = (JsonSchemaGeneratorBuilder) JsonSchemaGenerator.create().beanContextBuilder(getBeanContextBuilder());
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected JsonSchemaSerializerBuilder(JsonSchemaSerializer copyFrom) {
		super(copyFrom);
		generatorBuilder = (JsonSchemaGeneratorBuilder) copyFrom.generator.copy().beanContextBuilder(getBeanContextBuilder());
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected JsonSchemaSerializerBuilder(JsonSchemaSerializerBuilder copyFrom) {
		super(copyFrom);
		generatorBuilder = (JsonSchemaGeneratorBuilder) copyFrom.generatorBuilder.copy().beanContextBuilder(getBeanContextBuilder());
	}

	@Override /* ContextBuilder */
	public JsonSchemaSerializerBuilder copy() {
		return new JsonSchemaSerializerBuilder(this);
	}

	@Override /* ContextBuilder */
	public JsonSchemaSerializer build() {
		return (JsonSchemaSerializer)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Add descriptions.
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
	public JsonSchemaSerializerBuilder addDescriptionsTo(String value) {
		return set(JSONSCHEMA_addDescriptionsTo, value);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Add examples.
	 *
	 * <p>
	 * Identifies which categories of types that examples should be automatically added to generated schemas.
	 * <p>
	 * The examples come from calling {@link ClassMeta#getExample(BeanSession,JsonParserSession)} which in turn gets examples
	 * from the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
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
	public JsonSchemaSerializerBuilder addExamplesTo(String value) {
		return set(JSONSCHEMA_addExamplesTo, value);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
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
	public JsonSchemaSerializerBuilder allowNestedDescriptions() {
		return set(JSONSCHEMA_allowNestedDescriptions);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
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
	public JsonSchemaSerializerBuilder allowNestedExamples() {
		return set(JSONSCHEMA_allowNestedExamples);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
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
	public JsonSchemaSerializerBuilder beanDefMapper(Class<? extends BeanDefMapper> value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Bean schema definition mapper.
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
	public JsonSchemaSerializerBuilder beanDefMapper(BeanDefMapper value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Default schemas.
	 *
	 * <p>
	 * Allows you to override or provide custom schema information for particular class types.
	 * <p>
	 * Keys are full class names.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Schema#value}
	 * </ul>
	 *
	 * @param c
	 * 	The class to define a default schema for.
	 * @param schema
	 * 	The schema.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaSerializerBuilder defaultSchema(Class<?> c, OMap schema) {
		return annotations(SchemaAnnotation.create(c).value(schema.toString()).build());
	}

	/**
	 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
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
	public JsonSchemaSerializerBuilder useBeanDefs() {
		return set(JSONSCHEMA_useBeanDefs);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder apply(List<AnnotationWork> work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaSerializerBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaSerializerBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public JsonSchemaSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public JsonSchemaSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - JsonSerializerBuilder */
	public JsonSchemaSerializerBuilder escapeSolidus() {
		super.escapeSolidus();
		return this;
	}

	@Override /* GENERATED - JsonSerializerBuilder */
	public JsonSchemaSerializerBuilder simpleMode() {
		super.simpleMode();
		return this;
	}

	@Override /* GENERATED - JsonSerializerBuilder */
	public JsonSchemaSerializerBuilder ssq() {
		super.ssq();
		return this;
	}

	// </FluentSetters>
}