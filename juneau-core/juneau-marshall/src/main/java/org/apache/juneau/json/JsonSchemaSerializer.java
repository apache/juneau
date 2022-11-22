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

import static org.apache.juneau.collections.JsonMap.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.utils.*;

/**
 * Serializes POJO metadata to HTTP responses as JSON-Schema.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/json+schema, text/json+schema</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Produces the JSON-schema for the JSON produced by the {@link JsonSerializer} class with the same properties.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class JsonSchemaSerializer extends JsonSerializer implements JsonSchemaMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT = new JsonSchemaSerializer(create());

	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT_READABLE = new Readable(create());

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSchemaSerializer DEFAULT_SIMPLE = new Simple(create());

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSchemaSerializer DEFAULT_SIMPLE_READABLE = new SimpleReadable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Simple(Builder builder) {
			super(builder.simpleAttrs().quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SimpleReadable(Builder builder) {
			super(builder.simpleAttrs().quoteChar('\'').useWhitespace());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends JsonSerializer.Builder {

		private static final Cache<HashKey,JsonSchemaSerializer> CACHE = Cache.of(HashKey.class, JsonSchemaSerializer.class).build();

		JsonSchemaGenerator.Builder generatorBuilder;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("application/json");
			accept("application/json+schema,text/json+schema");
			generatorBuilder = JsonSchemaGenerator.create().beanContext(beanContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(JsonSchemaSerializer copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generator.copy().beanContext(beanContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generatorBuilder.copy().beanContext(beanContext());
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public JsonSchemaSerializer build() {
			return cache(CACHE).build(JsonSchemaSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				generatorBuilder.hashKey()
			);
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
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addDescriptionsTo(TypeCategory...values) {
			generatorBuilder.addDescriptionsTo(values);
			return this;
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
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addExamplesTo(TypeCategory...values) {
			generatorBuilder.addExamplesTo(values);
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedDescriptions() {
			generatorBuilder.allowNestedDescriptions();
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}
		 * </ul>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder allowNestedExamples() {
			generatorBuilder.allowNestedExamples();
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
		 *
		 * <p>
		 * Interface to use for converting Bean classes to definition IDs and URIs.
		 * <p>
		 * Used primarily for defining common definition sections for beans in Swagger JSON.
		 * <p>
		 * This setting is ignored if {@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder beanDefMapper(Class<? extends BeanDefMapper> value) {
			generatorBuilder.beanDefMapper(value);
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder useBeanDefs() {
			generatorBuilder.useBeanDefs();
			return this;
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

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder addBeanTypesJson() {
			super.addBeanTypesJson();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder addBeanTypesJson(boolean value) {
			super.addBeanTypesJson(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder escapeSolidus() {
			super.escapeSolidus();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder escapeSolidus(boolean value) {
			super.escapeSolidus(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder simpleAttrs() {
			super.simpleAttrs();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder simpleAttrs(boolean value) {
			super.simpleAttrs(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.json.JsonSerializer.Builder */
		public Builder json5() {
			super.json5();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final JsonSchemaGenerator generator;

	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSchemaSerializer(Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = builder.generatorBuilder.build();
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public JsonSchemaSerializerSession.Builder createSession() {
		return JsonSchemaSerializerSession.create(this);
	}

	@Override /* Context */
	public JsonSchemaSerializerSession getSession() {
		return createSession().build();
	}

	JsonSchemaGenerator getGenerator() {
		return generator;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsonSchemaMetaProvider */
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		JsonSchemaClassMeta m = jsonSchemaClassMetas.get(cm);
		if (m == null) {
			m = new JsonSchemaClassMeta(cm, this);
			jsonSchemaClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsonSchemaMetaProvider */
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		JsonSchemaBeanPropertyMeta m = jsonSchemaBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonSchemaBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsonSchemaBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap("generator", generator);
	}
}