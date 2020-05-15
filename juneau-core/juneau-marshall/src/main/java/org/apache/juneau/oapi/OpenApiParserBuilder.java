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

import static org.apache.juneau.oapi.OpenApiCommon.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of {@link OpenApiParser}.
 */
public class OpenApiParserBuilder extends UonParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public OpenApiParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public OpenApiParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public OpenApiParser build() {
		return build(OpenApiParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Default format for HTTP parts.
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link OpenApiCommon#OAPI_format}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public OpenApiParserBuilder format(HttpPartFormat value) {
		return set(OAPI_format, value);
	}

	/**
	 * Configuration property:  Default collection format for HTTP parts.
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link OpenApiCommon#OAPI_collectionFormat}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public OpenApiParserBuilder collectionFormat(HttpPartCollectionFormat value) {
		return set(OAPI_collectionFormat, value);
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

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
	public OpenApiParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder set(String name, Object value) {
		super.set(name, value);
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
	public OpenApiParserBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
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
	public OpenApiParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
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
	public OpenApiParserBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionary(java.lang.Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryRemove(java.lang.Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryReplace(java.lang.Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder examples(String json) {
		super.examples(json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder fluentSetters() {
		super.fluentSetters();
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
	public OpenApiParserBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClasses(java.lang.Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesRemove(java.lang.Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesReplace(java.lang.Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwaps(java.lang.Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsRemove(java.lang.Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsReplace(java.lang.Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
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
	public OpenApiParserBuilder decoding(boolean value) {
		super.decoding(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder validateEnd() {
		super.validateEnd();
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}