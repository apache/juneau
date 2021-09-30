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

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of {@link OpenApiParser}.
 */
@FluentSetters
public class OpenApiParserBuilder extends UonParserBuilder {

	HttpPartFormat format;
	HttpPartCollectionFormat collectionFormat;

	/**
	 * Constructor, default settings.
	 */
	protected OpenApiParserBuilder() {
		super();
		consumes("text/openapi");
		type(OpenApiParser.class);
		format = HttpPartFormat.NO_FORMAT;
		collectionFormat = HttpPartCollectionFormat.NO_COLLECTION_FORMAT;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected OpenApiParserBuilder(OpenApiParser copyFrom) {
		super(copyFrom);
		format = copyFrom.format;
		collectionFormat = copyFrom.collectionFormat;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected OpenApiParserBuilder(OpenApiParserBuilder copyFrom) {
		super(copyFrom);
		format = copyFrom.format;
		collectionFormat = copyFrom.collectionFormat;
	}

	@Override /* ContextBuilder */
	public OpenApiParserBuilder copy() {
		return new OpenApiParserBuilder(this);
	}

	@Override /* ContextBuilder */
	public OpenApiParser build() {
		return (OpenApiParser)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default format for HTTP parts.
	 *
	 * <p>
	 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#format()}.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a plain-text parser.</jc>
	 * 	OpenApiParser p1 = OpenApiParser
	 * 		.<jsm>create</jsm>()
	 * 		.build();
	 *
	 * 	<jc>// Create a UON parser.</jc>
	 * 	OpenApiParser p2 = OpenApiParser
	 * 		.<jsm>create</jsm>()
	 * 		.format(<jsf>UON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Parse a plain-text string.</jc>
	 * 	String v1 = p1.parse(<js>"foo bar"</js>);
	 *
	 * 	<jc>// Parse a UON string.</jc>
	 * 	String v2 = p2.parse(<js>"'foo bar'"</js>);
	 * </p>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public OpenApiParserBuilder format(HttpPartFormat value) {
		format = value;
		return this;
	}

	/**
	 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default collection format for HTTP parts.
	 *
	 * <p>
	 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#collectionFormat()}.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser using CSV for collections.</jc>
	 * 	OpenApiParser p1 = OpenApiParser
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>CSV</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Create a serializer using UON for collections.</jc>
	 * 	OpenApiParser p2 = OpenApiParser
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>UON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Parse CSV.</jc>
	 * 	OList l1 = p1.parse(<js>"foo=bar,baz=qux\,true\,123"</js>, OList.<jk>class</jk>)
	 *
	 * 	<jc>// Parse UON.</jc>
	 * 	OList l2 = p2.parse(<js>"(foo=bar,baz=@(qux,true,123))"</js>, OList.<jk>class</jk>)
	 * </p>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public OpenApiParserBuilder collectionFormat(HttpPartCollectionFormat value) {
		collectionFormat = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder swaps(Class<?>...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public OpenApiParserBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public OpenApiParserBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder decoding() {
		super.decoding();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder validateEnd() {
		super.validateEnd();
		return this;
	}

	// </FluentSetters>
}