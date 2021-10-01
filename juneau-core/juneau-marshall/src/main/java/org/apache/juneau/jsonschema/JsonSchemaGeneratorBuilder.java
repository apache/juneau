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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.internal.SystemEnv.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for building instances of JSON Schema generators.
 * {@review}
 */
@FluentSetters
public class JsonSchemaGeneratorBuilder extends BeanTraverseBuilder {

	final JsonSerializerBuilder jsonSerializerBuilder;
	final JsonParserBuilder jsonParserBuilder;

	Set<TypeCategory> addDescriptionsTo, addExamplesTo;
	boolean allowNestedDescriptions, allowNestedExamples, useBeanDefs;
	Class<? extends BeanDefMapper> beanDefMapper;
	Set<String> ignoreTypes;

	/**
	 * Constructor, default settings.
	 */
	protected JsonSchemaGeneratorBuilder() {
		super();
		BeanContextBuilder bc = beanContext();
		jsonSerializerBuilder = JsonSerializer.create().beanContext(bc);
		jsonParserBuilder = (JsonParserBuilder) JsonParser.create().beanContext(bc);
		type(JsonSchemaGenerator.class);
		registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
		addDescriptionsTo = null;
		addExamplesTo = null;
		allowNestedDescriptions = env("JsonSchemaGenerator.allowNestedDescriptions", false);
		allowNestedExamples = env("JsonSchemaGenerator.allowNestedExamples", false);
		useBeanDefs = env("JsonSchemaGenerator.useBeanDefs", false);
		beanDefMapper = BasicBeanDefMapper.class;
		ignoreTypes = null;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected JsonSchemaGeneratorBuilder(JsonSchemaGenerator copyFrom) {
		super(copyFrom);
		BeanContextBuilder bc = beanContext();
		jsonSerializerBuilder = copyFrom.jsonSerializer.copy().beanContext(bc);
		jsonParserBuilder = (JsonParserBuilder) copyFrom.jsonParser.copy().beanContext(bc);
		registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
		addDescriptionsTo = copyFrom.addDescriptionsTo.isEmpty() ? null : new TreeSet<>(copyFrom.addDescriptionsTo);
		addExamplesTo = copyFrom.addExamplesTo.isEmpty() ? null : new TreeSet<>(copyFrom.addExamplesTo);
		allowNestedDescriptions = copyFrom.allowNestedDescriptions;
		allowNestedExamples = copyFrom.allowNestedExamples;
		useBeanDefs = copyFrom.useBeanDefs;
		beanDefMapper = copyFrom.beanDefMapper;
		ignoreTypes = copyFrom.ignoreTypes.isEmpty() ? null : new TreeSet<>(copyFrom.ignoreTypes);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected JsonSchemaGeneratorBuilder(JsonSchemaGeneratorBuilder copyFrom) {
		super(copyFrom);
		BeanContextBuilder bc = beanContext();
		jsonSerializerBuilder = copyFrom.jsonSerializerBuilder.copy().beanContext(bc);
		jsonParserBuilder = (JsonParserBuilder) copyFrom.jsonParserBuilder.copy().beanContext(bc);
		registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
		addDescriptionsTo = copyFrom.addDescriptionsTo == null ? null : new TreeSet<>(copyFrom.addDescriptionsTo);
		addExamplesTo = copyFrom.addExamplesTo == null ? null : new TreeSet<>(copyFrom.addExamplesTo);
		allowNestedDescriptions = copyFrom.allowNestedDescriptions;
		allowNestedExamples = copyFrom.allowNestedExamples;
		useBeanDefs = copyFrom.useBeanDefs;
		beanDefMapper = copyFrom.beanDefMapper;
		ignoreTypes = copyFrom.ignoreTypes == null ? null : new TreeSet<>(copyFrom.ignoreTypes);
	}

	@Override /* ContextBuilder */
	public JsonSchemaGeneratorBuilder copy() {
		return new JsonSchemaGeneratorBuilder(this);
	}

	@Override /* ContextBuilder */
	public JsonSchemaGenerator build() {
		return (JsonSchemaGenerator)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions.
	 *
	 * <p>
	 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
	 * The description is the result of calling {@link ClassMeta#getFullName()}.
	 * The format is a comma-delimited list of any of the following values:
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
	 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
	 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
	 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
	 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
	 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
	 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
	 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
	 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this setting.
	 * 	<br>The default is an empty string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder addDescriptionsTo(TypeCategory...values) {
		if (addDescriptionsTo == null)
			addDescriptionsTo = new TreeSet<>();
		Collections.addAll(addDescriptionsTo, values);
		return this;
	}

	/**
	 * Add examples.
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
	 * <p>
	 * The format is a comma-delimited list of any of the following values:
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
	 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
	 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
	 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
	 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
	 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
	 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
	 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
	 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this setting.
	 * 	<br>The default is an empty string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder addExamplesTo(TypeCategory...values) {
		if (addExamplesTo == null)
			addExamplesTo = new TreeSet<>();
		Collections.addAll(addExamplesTo, values);
		return this;
	}

	/**
	 * Allow nested descriptions.
	 *
	 * <p>
	 * Identifies whether nested descriptions are allowed in schema definitions.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder allowNestedDescriptions() {
		return allowNestedDescriptions(true);
	}

	/**
	 * Same as {@link #allowNestedDescriptions()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder allowNestedDescriptions(boolean value) {
		allowNestedDescriptions = value;
		return this;
	}

	/**
	 * Allow nested examples.
	 *
	 * <p>
	 * Identifies whether nested examples are allowed in schema definitions.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder allowNestedExamples() {
		return allowNestedExamples(true);
	}

	/**
	 * Same as {@link #allowNestedExamples()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder allowNestedExamples(boolean value) {
		allowNestedExamples = value;
		return this;
	}

	/**
	 * Schema definition mapper.
	 *
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 * <p>
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 * <p>
	 * This setting is ignored if {@link JsonSchemaGeneratorBuilder#useBeanDefs()} is not enabled.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder beanDefMapper(Class<? extends BeanDefMapper> value) {
		beanDefMapper = value;
		return this;
	}

	/**
	 * Default schemas.
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
	public JsonSchemaGeneratorBuilder defaultSchema(Class<?> c, OMap schema) {
		return annotations(SchemaAnnotation.create(c.getName()).value(schema.toString()).build());
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.</jc>
	 * 	<ja>@JsonSchemaConfig</ja>(
	 * 		ignoreTypes=<js>"Swagger,*.proto.*"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * @param values
	 * 	The values to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder ignoreTypes(String...values) {
		if (ignoreTypes == null)
			ignoreTypes = new TreeSet<>();
		Collections.addAll(ignoreTypes, values);
		return this;
	}

	/**
	 * Use bean definitions.
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
	 * <p>
	 * The definitions can then be retrieved from the session using {@link JsonSchemaGeneratorSession#getBeanDefs()}.
	 * <p>
	 * Definitions can also be added programmatically using {@link JsonSchemaGeneratorSession#addBeanDef(String, OMap)}.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder useBeanDefs() {
		return useBeanDefs(true);
	}

	/**
	 * Same as {@link #useBeanDefs()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder useBeanDefs(boolean value) {
		useBeanDefs = value;
		return this;
	}

	/**
	 * Gives access to the inner JSON serializer builder if you want to modify the serializer settings.
	 *
	 * @return The JSON serializer builder.
	 */
	public JsonSerializerBuilder getJsonSerializerBuilder() {
		return jsonSerializerBuilder;
	}

	/**
	 * Gives access to the inner JSON parser builder if you want to modify the parser settings.
	 *
	 * @return The JSON serializer builder.
	 */
	public JsonParserBuilder getJsonParserBuilder() {
		return jsonParserBuilder;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder beanContext(BeanContext value) {
		super.beanContext(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaGeneratorBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaGeneratorBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder swaps(Class<?>...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaGeneratorBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaGeneratorBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaGeneratorBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public JsonSchemaGeneratorBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}
}
