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

import static org.apache.juneau.jsonschema.JsonSchemaGenerator.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building instances of JSON Schema generators.
 */
public class JsonSchemaGeneratorBuilder extends BeanTraverseBuilder {

	/**
	 * Constructor, default settings.
	 */
	public JsonSchemaGeneratorBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public JsonSchemaGeneratorBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public JsonSchemaGenerator build() {
		return build(JsonSchemaGenerator.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Add descriptions.
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
	public JsonSchemaGeneratorBuilder addDescriptionsTo(String value) {
		return set(JSONSCHEMA_addDescriptionsTo, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Add examples.
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
	public JsonSchemaGeneratorBuilder addExamplesTo(String value) {
		return set(JSONSCHEMA_addExamplesTo, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Allow nested descriptions.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #allowNestedDescriptions()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public JsonSchemaGeneratorBuilder allowNestedDescriptions(boolean value) {
		return set(JSONSCHEMA_allowNestedDescriptions, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Allow nested descriptions.
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
	public JsonSchemaGeneratorBuilder allowNestedDescriptions() {
		return set(JSONSCHEMA_allowNestedDescriptions, true);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Allow nested examples.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #allowNestedExamples()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public JsonSchemaGeneratorBuilder allowNestedExamples(boolean value) {
		return set(JSONSCHEMA_allowNestedExamples, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Allow nested examples.
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
	public JsonSchemaGeneratorBuilder allowNestedExamples() {
		return set(JSONSCHEMA_allowNestedExamples, true);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Schema definition mapper.
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
	public JsonSchemaGeneratorBuilder beanDefMapper(Class<? extends BeanDefMapper> value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Bean schema definition mapper.
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
	public JsonSchemaGeneratorBuilder beanDefMapper(BeanDefMapper value) {
		return set(JSONSCHEMA_beanDefMapper, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Default schemas.
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
	public JsonSchemaGeneratorBuilder defaultSchema(Class<?> c, OMap schema) {
		return putTo(JSONSCHEMA_defaultSchemas, c.getName(), schema);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Ignore types from schema definitions.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.</jc>
	 * 	<ja>@Rest</ja>(
	 * 			properties={
	 * 				<ja>@Property</ja>(name=<jsf>JSONSCHEMA_ignoreTypes</jsf>, value=<js>"Swagger,*.proto.*"</js>)
	 * 			}
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 *
	 * @param value
	 * 	A comma-delimited list of types to ignore.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public JsonSchemaGeneratorBuilder ignoreTypes(String value) {
		return set(JSONSCHEMA_ignoreTypes, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Use bean definitions.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #useBeanDefs()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@FluentSetter
	@Deprecated
	public JsonSchemaGeneratorBuilder useBeanDefs(boolean value) {
		return set(JSONSCHEMA_useBeanDefs, value);
	}

	/**
	 * <i><l>JsonSchemaGenerator</l> configuration property:</i>  Use bean definitions.
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
		return set(JSONSCHEMA_useBeanDefs, true);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
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
	public JsonSchemaGeneratorBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonSchemaGeneratorBuilder set(String name, Object value) {
		super.set(name, value);
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
	public JsonSchemaGeneratorBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
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
	public JsonSchemaGeneratorBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaGeneratorBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonSchemaGeneratorBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
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
	public JsonSchemaGeneratorBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder notBeanPackages(Object...values) {
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
	public JsonSchemaGeneratorBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonSchemaGeneratorBuilder timeZone(TimeZone value) {
		super.timeZone(value);
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

	// </FluentSetters>
}
