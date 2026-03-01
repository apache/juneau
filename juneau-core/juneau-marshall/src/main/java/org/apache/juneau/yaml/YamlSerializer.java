/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.yaml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to YAML.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/yaml, text/yaml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/yaml</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to YAML mappings.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		YAML sequences.
 * 	<li>
 * 		{@link String Strings} are converted to YAML scalars.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to YAML numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to YAML booleans.
 * 	<li>
 * 		{@code nulls} are converted to YAML nulls.
 * 	<li>
 * 		{@code arrays} are converted to YAML sequences.
 * 	<li>
 * 		{@code beans} are converted to YAML mappings.
 * </ul>
 *
 * <p>
 * The types above are considered "YAML-primitive" object types.
 * Any non-YAML-primitive object types are transformed into YAML-primitive object types through
 * {@link org.apache.juneau.swap.ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.BeanContext.Builder#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>yaml</jv> = YamlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	YamlSerializer <jv>serializer</jv> = YamlSerializer.<jsm>create</jsm>().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use whitespace</jc>
 * 	<jv>serializer</jv> = YamlSerializer.<jsf>DEFAULT</jsf>.copy().ws().build();
 *
 * 	<jc>// Serialize a POJO to YAML</jc>
 * 	String <jv>yaml</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * The YAML serializer has fewer configuration options than {@link org.apache.juneau.json.JsonSerializer JsonSerializer}:
 * <ul class='spaced-list'>
 * 	<li>
 * 		No compact single-line output mode; YAML is always emitted in block-style (indentation-based) format.
 * 	<li>
 * 		No equivalent to JSON's simple mode or attribute quoting style variants (single vs double quotes).
 * 	<li>
 * 		No strict vs lax output modes; YAML output follows a consistent, human-readable style.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/YamlBasics">YAML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class YamlSerializer extends WriterSerializer {

	private static final String PROP_addBeanTypesYaml = "addBeanTypesYaml";

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,YamlSerializer> CACHE = Cache.of(HashKey.class, YamlSerializer.class).build();

		private boolean addBeanTypesYaml;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/yaml");
			accept("application/yaml,text/yaml");
			addBeanTypesYaml = env("YamlSerializer.addBeanTypes", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesYaml = copyFrom.addBeanTypesYaml;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(YamlSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesYaml = copyFrom.addBeanTypesYaml;
		}

		@Override /* Overridden from Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesYaml() {
			return addBeanTypesYaml(true);
		}

		/**
		 * Same as {@link #addBeanTypesYaml()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesYaml(boolean value) {
			addBeanTypesYaml = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Class<?>...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder applyAnnotations(Object...from) {
			super.applyAnnotations(from);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public YamlSerializer build() {
			return cache(CACHE).build(YamlSerializer.class);
		}

		@Override /* Overridden from Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* Overridden from Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				addBeanTypesYaml
			);
		}

		@Override /* Overridden from Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* Overridden from Builder */
		public <T,S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder swaps(Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder swaps(Object...values) {
			super.swaps(values);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder ws() {
			super.ws();
			return this;
		}
	}

	/** Default serializer, with whitespace. */
	public static class Readable extends YamlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, all default settings. */
	public static final YamlSerializer DEFAULT = new YamlSerializer(create());

	/** Default serializer, with whitespace. */
	public static final YamlSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean addBeanTypesYaml;

	private final boolean addBeanTypes2;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public YamlSerializer(Builder builder) {
		super(builder);
		addBeanTypesYaml = builder.addBeanTypesYaml;
		addBeanTypes2 = addBeanTypesYaml || super.isAddBeanTypes();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public YamlSerializerSession.Builder createSession() {
		return YamlSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public YamlSerializerSession getSession() { return createSession().build(); }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesYaml()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypes2; }

	@Override /* Overridden from WriterSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesYaml, addBeanTypesYaml);
	}
}
