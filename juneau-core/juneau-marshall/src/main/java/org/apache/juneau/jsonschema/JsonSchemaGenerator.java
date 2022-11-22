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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.Collections.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.utils.*;

/**
 * Generates JSON-schema metadata about POJOs.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
public class JsonSchemaGenerator extends BeanTraverseContext implements JsonSchemaMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaGenerator DEFAULT = new JsonSchemaGenerator(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanTraverseContext.Builder {

		private static final Cache<HashKey,JsonSchemaGenerator> CACHE = Cache.of(HashKey.class, JsonSchemaGenerator.class).build();

		final JsonSerializer.Builder jsonSerializerBuilder;
		final JsonParser.Builder jsonParserBuilder;

		SortedSet<TypeCategory> addDescriptionsTo, addExamplesTo;
		boolean allowNestedDescriptions, allowNestedExamples, useBeanDefs;
		Class<? extends BeanDefMapper> beanDefMapper;
		SortedSet<String> ignoreTypes;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			BeanContext.Builder bc = beanContext();
			jsonSerializerBuilder = JsonSerializer.create().beanContext(bc);
			jsonParserBuilder = JsonParser.create().beanContext(bc);
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
		protected Builder(JsonSchemaGenerator copyFrom) {
			super(copyFrom);
			BeanContext.Builder bc = beanContext();
			jsonSerializerBuilder = copyFrom.jsonSerializer.copy().beanContext(bc);
			jsonParserBuilder = copyFrom.jsonParser.copy().beanContext(bc);
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
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			BeanContext.Builder bc = beanContext();
			jsonSerializerBuilder = copyFrom.jsonSerializerBuilder.copy().beanContext(bc);
			jsonParserBuilder = copyFrom.jsonParserBuilder.copy().beanContext(bc);
			registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
			addDescriptionsTo = copyFrom.addDescriptionsTo == null ? null : new TreeSet<>(copyFrom.addDescriptionsTo);
			addExamplesTo = copyFrom.addExamplesTo == null ? null : new TreeSet<>(copyFrom.addExamplesTo);
			allowNestedDescriptions = copyFrom.allowNestedDescriptions;
			allowNestedExamples = copyFrom.allowNestedExamples;
			useBeanDefs = copyFrom.useBeanDefs;
			beanDefMapper = copyFrom.beanDefMapper;
			ignoreTypes = copyFrom.ignoreTypes == null ? null : new TreeSet<>(copyFrom.ignoreTypes);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public JsonSchemaGenerator build() {
			return cache(CACHE).build(JsonSchemaGenerator.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				jsonSerializerBuilder.hashKey(),
				jsonParserBuilder.hashKey(),
				addDescriptionsTo,
				addExamplesTo,
				allowNestedDescriptions,
				allowNestedExamples,
				useBeanDefs,
				beanDefMapper,
				ignoreTypes
			);
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder addDescriptionsTo(TypeCategory...values) {
			addDescriptionsTo = addAll(addDescriptionsTo, values);
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder addExamplesTo(TypeCategory...values) {
			addExamplesTo = addAll(addExamplesTo, values);
			return this;
		}

		/**
		 * Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedDescriptions() {
			return allowNestedDescriptions(true);
		}

		/**
		 * Same as {@link #allowNestedDescriptions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedDescriptions(boolean value) {
			allowNestedDescriptions = value;
			return this;
		}

		/**
		 * Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedExamples() {
			return allowNestedExamples(true);
		}

		/**
		 * Same as {@link #allowNestedExamples()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedExamples(boolean value) {
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
		 * This setting is ignored if {@link JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanDefMapper(Class<? extends BeanDefMapper> value) {
			beanDefMapper = value;
			return this;
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
		 * <p class='bjava'>
		 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.</jc>
		 * 	<ja>@JsonSchemaConfig</ja>(
		 * 		ignoreTypes=<js>"Swagger,*.proto.*"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {...}
		 * </p>
		 *
		 * @param values
		 * 	The values to add.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreTypes(String...values) {
			ignoreTypes = addAll(ignoreTypes, values);
			return this;
		}

		/**
		 * Use bean definitions.
		 *
		 * <p>
		 * When enabled, schemas on beans will be serialized as the following:
		 * <p class='bjson'>
		 * 	{
		 * 		type: <js>'object'</js>,
		 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
		 * 	}
		 * </p>
		 *
		 * <p>
		 * The definitions can then be retrieved from the session using {@link JsonSchemaGeneratorSession#getBeanDefs()}.
		 * <p>
		 * Definitions can also be added programmatically using {@link JsonSchemaGeneratorSession#addBeanDef(String, JsonMap)}.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder useBeanDefs() {
			return useBeanDefs(true);
		}

		/**
		 * Same as {@link #useBeanDefs()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder useBeanDefs(boolean value) {
			useBeanDefs = value;
			return this;
		}

		/**
		 * Gives access to the inner JSON serializer builder if you want to modify the serializer settings.
		 *
		 * @return The JSON serializer builder.
		 */
		public JsonSerializer.Builder getJsonSerializerBuilder() {
			return jsonSerializerBuilder;
		}

		/**
		 * Gives access to the inner JSON parser builder if you want to modify the parser settings.
		 *
		 * @return The JSON serializer builder.
		 */
		public JsonParser.Builder getJsonParserBuilder() {
			return jsonParserBuilder;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean useBeanDefs, allowNestedExamples, allowNestedDescriptions;
	final Set<TypeCategory> addExamplesTo, addDescriptionsTo;
	final Class<? extends BeanDefMapper> beanDefMapper;
	final Set<String> ignoreTypes;

	private final BeanDefMapper beanDefMapperBean;
	final JsonSerializer jsonSerializer;
	final JsonParser jsonParser;
	private final Pattern[] ignoreTypePatterns;
	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSchemaGenerator(Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		useBeanDefs = builder.useBeanDefs;
		allowNestedExamples = builder.allowNestedExamples;
		allowNestedDescriptions = builder.allowNestedDescriptions;
		beanDefMapper = builder.beanDefMapper;
		addExamplesTo = builder.addExamplesTo == null ? emptySet() : new TreeSet<>(builder.addExamplesTo);
		addDescriptionsTo = builder.addDescriptionsTo == null ? emptySet() : new TreeSet<>(builder.addDescriptionsTo);
		ignoreTypes = builder.ignoreTypes == null ? emptySet() : new TreeSet<>(builder.ignoreTypes);

		Set<Pattern> ignoreTypePatterns = set();
		ignoreTypes.forEach(y -> split(y, x -> ignoreTypePatterns.add(Pattern.compile(x.replace(".", "\\.").replace("*", ".*")))));
		this.ignoreTypePatterns = ignoreTypePatterns.toArray(new Pattern[ignoreTypePatterns.size()]);

		try {
			beanDefMapperBean = beanDefMapper.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw asRuntimeException(e);
		}

		jsonSerializer = builder.jsonSerializerBuilder.build();
		jsonParser = builder.jsonParserBuilder.beanContext(getBeanContext()).build();
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public JsonSchemaGeneratorSession.Builder createSession() {
		return JsonSchemaGeneratorSession.create(this);
	}

	@Override /* Context */
	public JsonSchemaGeneratorSession getSession() {
		return createSession().build();
	}

	JsonSerializer getJsonSerializer() {
		return jsonSerializer;
	}

	JsonParser getJsonParser() {
		return jsonParser;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions to types.
	 *
	 * @see Builder#addDescriptionsTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return addDescriptionsTo;
	}

	/**
	 * Add examples.
	 *
	 * @see Builder#addExamplesTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return addExamplesTo;
	}

	/**
	 * Allow nested descriptions.
	 *
	 * @see Builder#allowNestedDescriptions()
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return allowNestedDescriptions;
	}

	/**
	 * Allow nested examples.
	 *
	 * @see Builder#allowNestedExamples()
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return allowNestedExamples;
	}

	/**
	 * Bean schema definition mapper.
	 *
	 * @see Builder#beanDefMapper(Class)
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return beanDefMapperBean;
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * @see Builder#ignoreTypes(String...)
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	public List<Pattern> getIgnoreTypes() {
		return ulist(ignoreTypePatterns);
	}

	/**
	 * Use bean definitions.
	 *
	 * @see Builder#useBeanDefs()
	 * @return
	 * 	<jk>true</jk> if schemas on beans will be serialized with <js>'$ref'</js> tags.
	 */
	protected final boolean isUseBeanDefs() {
		return useBeanDefs;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		JsonSchemaClassMeta m = jsonSchemaClassMetas.get(cm);
		if (m == null) {
			m = new JsonSchemaClassMeta(cm, this);
			jsonSchemaClassMetas.put(cm, m);
		}
		return m;
	}

	@Override
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		JsonSchemaBeanPropertyMeta m = jsonSchemaBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonSchemaBeanPropertyMeta(bpm, this);
			jsonSchemaBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the specified type is ignored.
	 *
	 * <p>
	 * The type is ignored if it's specified in the {@link Builder#ignoreTypes(String...)} setting.
	 * <br>Ignored types return <jk>null</jk> on the call to {@link JsonSchemaGeneratorSession#getSchema(ClassMeta)}.
	 *
	 * @param cm The type to check.
	 * @return <jk>true</jk> if the specified type is ignored.
	 */
	public boolean isIgnoredType(ClassMeta<?> cm) {
		for (Pattern p : ignoreTypePatterns)
			if (p.matcher(cm.getSimpleName()).matches() || p.matcher(cm.getName()).matches())
				return true;
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("useBeanDefs", useBeanDefs)
			.append("allowNestedExamples", allowNestedExamples)
			.append("allowNestedDescriptions", allowNestedDescriptions)
			.append("beanDefMapper", beanDefMapper)
			.append("addExamplesTo", addExamplesTo)
			.append("addDescriptionsTo", addDescriptionsTo)
			.append("ignoreTypes", ignoreTypes);
	}
}
