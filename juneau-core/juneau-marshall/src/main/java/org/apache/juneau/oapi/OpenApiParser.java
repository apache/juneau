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
 * OpenAPI part parser.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class OpenApiParser extends UonParser implements OpenApiMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiParser}. */
	public static final OpenApiParser DEFAULT = new OpenApiParser(create());

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
	public static class Builder extends UonParser.Builder {

		private static final Cache<HashKey,OpenApiParser> CACHE = Cache.of(HashKey.class, OpenApiParser.class).build();

		HttpPartFormat format;
		HttpPartCollectionFormat collectionFormat;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			consumes("text/openapi");
			format = HttpPartFormat.NO_FORMAT;
			collectionFormat = HttpPartCollectionFormat.NO_COLLECTION_FORMAT;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(OpenApiParser copyFrom) {
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
		public OpenApiParser build() {
			return cache(CACHE).build(OpenApiParser.class);
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
		 * 	<jc>// Create a plain-text parser.</jc>
		 * 	OpenApiParser <jv>parser1</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create a UON parser.</jc>
		 * 	OpenApiParser <jv>parser2</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.format(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Parse a plain-text string.</jc>
		 * 	String <jv>value1</jv> = <jv>parser1</jv>.parse(<js>"foo bar"</js>);
		 *
		 * 	<jc>// Parse a UON string.</jc>
		 * 	String <jv>value2</jv> = <jv>parser1</jv>.parse(<js>"'foo bar'"</js>);
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
		 * 	<jc>// Create a parser using CSV for collections.</jc>
		 * 	OpenApiParser <jv>parser1</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>CSV</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Create a serializer using UON for collections.</jc>
		 * 	OpenApiParser <jv>parser2</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Parse CSV.</jc>
		 * 	JsonList <jv>list1</jv> = <jv>parser1</jv>.parse(<js>"foo=bar,baz=qux\,true\,123"</js>, JsonList.<jk>class</jk>)
		 *
		 * 	<jc>// Parse UON.</jc>
		 * 	JsonList <jv>list2</jv> = <jv>parser2</jv>.parse(<js>"(foo=bar,baz=@(qux,true,123))"</js>, JsonList.<jk>class</jk>)
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

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder autoCloseStreams() {
			super.autoCloseStreams();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder autoCloseStreams(boolean value) {
			super.autoCloseStreams(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder debugOutputLines(int value) {
			super.debugOutputLines(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder strict() {
			super.strict();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder strict(boolean value) {
			super.strict(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder unbuffered() {
			super.unbuffered();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.Parser.Builder */
		public Builder unbuffered(boolean value) {
			super.unbuffered(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParser.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParser.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonParser.Builder */
		public Builder decoding() {
			super.decoding();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonParser.Builder */
		public Builder decoding(boolean value) {
			super.decoding(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonParser.Builder */
		public Builder validateEnd() {
			super.validateEnd();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.uon.UonParser.Builder */
		public Builder validateEnd(boolean value) {
			super.validateEnd(value);
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
	 * @param builder The builder for this object.
	 */
	public OpenApiParser(Builder builder) {
		super(builder);
		format = builder.format;
		collectionFormat = builder.collectionFormat;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public OpenApiParserSession.Builder createSession() {
		return OpenApiParserSession.create(this);
	}

	@Override /* Context */
	public OpenApiParserSession getSession() {
		return createSession().build();
	}

	@Override /* HttpPartParser */
	public OpenApiParserSession getPartSession() {
		return OpenApiParserSession.create(this).build();
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
