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
package org.apache.juneau.oapi;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class OpenApiSerializer extends UonSerializer implements OpenApiMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiSerializer}, all default settings. */
	public static final OpenApiSerializer DEFAULT = new OpenApiSerializer(create());

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
	public static class Builder extends UonSerializer.Builder {

		private static final Cache<HashKey,OpenApiSerializer> CACHE = Cache.of(HashKey.class, OpenApiSerializer.class).build();

		HttpPartFormat format;
		HttpPartCollectionFormat collectionFormat;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("text/openapi");
			format = HttpPartFormat.NO_FORMAT;
			collectionFormat = HttpPartCollectionFormat.NO_COLLECTION_FORMAT;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(OpenApiSerializer copyFrom) {
			super(copyFrom);
			format = copyFrom.format;
			collectionFormat = copyFrom.collectionFormat;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			format = copyFrom.format;
			collectionFormat = copyFrom.collectionFormat;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public OpenApiSerializer build() {
			return cache(CACHE).build(OpenApiSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				format,
				collectionFormat
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default format for HTTP parts.
		 *
		 * <p>
		 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#format()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a plain-text serializer.</jc>
		 * 	OpenApiSerializer <jv>serializer1</jv> = OpenApiSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create a UON serializer.</jc>
		 * 	OpenApiSerializer <jv>serializer2</jv> = OpenApiSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.format(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	String <jv>string</jv> = <js>"foo bar"</js>;
		 *
		 * 	<jc>// Produces: "foo bar"</jc>
		 * 	String <jv>value1</jv> = <jv>serializer1</jv>.serialize(<jv>string</jv>);
		 *
		 * 	<jc>// Produces: "'foo bar'"</jc>
		 * 	String <jv>value2</jv> = <jv>serializer2</jv>.serialize(<jv>string</jv>);
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT32 INT32} - Signed 32 bits.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT64 INT64} - Signed 64 bits.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
		 * 	</ul>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder format(HttpPartFormat value) {
			format = value;
			return this;
		}

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default collection format for HTTP parts.
		 *
		 * <p>
		 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.annotation.Schema#collectionFormat()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer using CSV for collections.</jc>
		 * 	OpenApiSerializer <jv>serializer1</jv> = OpenApiSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>CSV</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Create a serializer using UON for collections.</jc>
		 * 	OpenApiSerializer <jv>serializer2</jv> = OpenApiSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// An arbitrary data structure.</jc>
		 * 	JsonList <jv>list</jv> = JsonList.<jsm>of</jsm>(
		 * 		<js>"foo"</js>,
		 * 		<js>"bar"</js>,
		 * 		JsonMap.<jsm>of</jsm>(
		 * 			<js>"baz"</js>, JsonList.<jsm>of</jsm>(<js>"qux"</js>,<js>"true"</js>,<js>"123"</js>)
		 *		)
		 *	);
		 *
		 * 	<jc>// Produces: "foo=bar,baz=qux\,true\,123"</jc>
		 * 	String <jv>value1</jv> = <jv>serializer1</jv>.serialize(<jv>list</jv>)
		 *
		 * 	<jc>// Produces: "(foo=bar,baz=@(qux,'true','123'))"</jc>
		 * 	String <jv>value2</jv> = <jv>serializer2</jv>.serialize(<jv>list</jv>)
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
		 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
		 * 	</ul>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder collectionFormat(HttpPartCollectionFormat value) {
			collectionFormat = value;
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

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder addBeanTypesUon() {
			super.addBeanTypesUon();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder addBeanTypesUon(boolean value) {
			super.addBeanTypesUon(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder encoding() {
			super.encoding();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder paramFormat(ParamFormat value) {
			super.paramFormat(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder paramFormatPlain() {
			super.paramFormatPlain();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonSerializer.Builder */
		public Builder quoteCharUon(char value) {
			super.quoteCharUon(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HttpPartFormat format;
	final HttpPartCollectionFormat collectionFormat;

	private final Map<ClassMeta<?>,OpenApiClassMeta> openApiClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,OpenApiBeanPropertyMeta> openApiBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	public OpenApiSerializer(Builder builder) {
		super(builder.encoding(false));
		format = builder.format;
		collectionFormat = builder.collectionFormat;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public OpenApiSerializerSession.Builder createSession() {
		return OpenApiSerializerSession.create(this);
	}

	@Override /* Context */
	public OpenApiSerializerSession getSession() {
		return createSession().build();
	}

	@Override /* HttpPartSerializer */
	public OpenApiSerializerSession getPartSession() {
		return OpenApiSerializerSession.create(this).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* OpenApiMetaProvider */
	public OpenApiClassMeta getOpenApiClassMeta(ClassMeta<?> cm) {
		OpenApiClassMeta m = openApiClassMetas.get(cm);
		if (m == null) {
			m = new OpenApiClassMeta(cm, this);
			openApiClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* OpenApiMetaProvider */
	public OpenApiBeanPropertyMeta getOpenApiBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return OpenApiBeanPropertyMeta.DEFAULT;
		OpenApiBeanPropertyMeta m = openApiBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new OpenApiBeanPropertyMeta(bpm.getDelegateFor(), this);
			openApiBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default format to use when not otherwise specified via {@link Schema#format()}
	 *
	 * @return The default format to use when not otherwise specified via {@link Schema#format()}
	 */
	protected final HttpPartFormat getFormat() {
		return format;
	}

	/**
	 * Returns the default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 *
	 * @return The default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 */
	protected final HttpPartCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}
}
