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
package org.apache.juneau.markdown;

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to Markdown (fragment mode: tables and lists).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/markdown, text/x-markdown</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/markdown</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows:
 * <ul class='spaced-list'>
 * 	<li>Beans and Maps are serialized as 2-column key/value Markdown tables.
 * 	<li>Collections and arrays of beans/maps with uniform structure are serialized as multi-column Markdown tables.
 * 	<li>Collections and arrays of simple values are serialized as Markdown bulleted lists.
 * 	<li>Nested beans and complex values in table cells are serialized as inline JSON5 wrapped in backticks.
 * 	<li>Null values are rendered as <js>"*null*"</js> (italic) by default.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default serializer to serialize a bean</jc>
 * 	String <jv>md</jv> = MarkdownSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	MarkdownSerializer <jv>serializer</jv> = MarkdownSerializer.<jsm>create</jsm>().nullValue(<js>"—"</js>).build();
 *
 * 	<jc>// Serialize a List of beans to a multi-column Markdown table</jc>
 * 	String <jv>md</jv> = <jv>serializer</jv>.serialize(<jv>listOfBeans</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean):</h5>
 * <p class='bcode'>
 * 	| Property | Value |
 * 	|---|---|
 * 	| name | Alice |
 * 	| age | 30 |
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Multi-line string values cannot round-trip; newlines are replaced with {@code <br>}.
 * 	<li class='note'>The configured null value string (default: {@code *null*}) cannot be stored as a literal string value.
 * 	<li class='note'>This serializer is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarkdownBasics">Markdown Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class MarkdownSerializer extends WriterSerializer implements MarkdownMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";
	private static final String CONST_null = "*null*";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,MarkdownSerializer> CACHE = Cache.of(HashKey.class, MarkdownSerializer.class).build();

		String nullValue;
		boolean showHeaders;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/markdown");
			accept("text/markdown,text/x-markdown");
			nullValue = CONST_null;
			showHeaders = true;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
			showHeaders = copyFrom.showHeaders;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarkdownSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
			showHeaders = copyFrom.showHeaders;
		}

		/**
		 * The string to render for null values.
		 *
		 * <p>
		 * Default is {@code *null*} (rendered as Markdown italic).
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value == null ? CONST_null : value;
			return this;
		}

		/**
		 * Whether to show table header rows.
		 *
		 * <p>
		 * Default is <jk>true</jk>.
		 *
		 * @param value Whether to show headers.
		 * @return This object.
		 */
		public Builder showHeaders(boolean value) {
			showHeaders = value;
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullValue, showHeaders);
		}

		@Override /* Overridden from Context.Builder */
		public MarkdownSerializer build() {
			return cache(CACHE).build(MarkdownSerializer.class);
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

	/**
	 * Default serializer, all default settings.
	 */
	public static final MarkdownSerializer DEFAULT = new MarkdownSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,MarkdownClassMeta> markdownClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,MarkdownBeanPropertyMeta> markdownBeanPropertyMetas = new ConcurrentHashMap<>();
	final String nullValue;
	final boolean showHeaders;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MarkdownSerializer(Builder builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : CONST_null;
		showHeaders = builder.showHeaders;
	}

	/**
	 * Returns the string rendered for null values.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	/**
	 * Returns whether table headers are shown.
	 *
	 * @return Whether headers are shown.
	 */
	public boolean isShowHeaders() {
		return showHeaders;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public MarkdownSerializerSession.Builder createSession() {
		return MarkdownSerializerSession.create(this);
	}

	@Override /* Overridden from MarkdownMetaProvider */
	public MarkdownBeanPropertyMeta getMarkdownBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return MarkdownBeanPropertyMeta.DEFAULT;
		return markdownBeanPropertyMetas.computeIfAbsent(bpm, k -> new MarkdownBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from MarkdownMetaProvider */
	public MarkdownClassMeta getMarkdownClassMeta(ClassMeta<?> cm) {
		return markdownClassMetas.computeIfAbsent(cm, k -> new MarkdownClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public MarkdownSerializerSession getSession() { return createSession().build(); }
}
