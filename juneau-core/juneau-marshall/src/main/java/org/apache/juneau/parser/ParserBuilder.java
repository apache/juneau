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
package org.apache.juneau.parser;

import static org.apache.juneau.parser.Parser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of parsers.
 */
public class ParserBuilder extends BeanContextBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public ParserBuilder(PropertyStore ps) {
		super(ps);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Parser</l> configuration property:</i>  Auto-close streams.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #autoCloseStreams()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public ParserBuilder autoCloseStreams(boolean value) {
		return set(PARSER_autoCloseStreams, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Auto-close streams.
	 *
	 * <p>
	 * When enabled, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser using strict mode.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.autoCloseStreams()
	 * 		.build();
	 *
	 * 	Reader r = <jk>new</jk> FileReader(<js>"/tmp/myfile.json"</js>);
	 * 	MyBean myBean = p.parse(r, MyBean.<jk>class</jk>);
	 *
	 * 	<jsm>assertTrue</jsm>(r.isClosed());
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder autoCloseStreams() {
		return set(PARSER_autoCloseStreams, true);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Debug output lines.
	 *
	 * <p>
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser whose exceptions print out 100 lines before and after the parse error location.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.debug()  <jc>// Enable debug mode to capture Reader contents as strings.</jc>
	 * 		.debugOuputLines(100)
	 * 		.build();
	 *
	 * 	Reader r = <jk>new</jk> FileReader(<js>"/tmp/mybadfile.json"</js>);
	 * 	<jk>try</jk> {
	 * 		p.parse(r, Object.<jk>class</jk>);
	 * 	} <jk>catch</jk> (ParseException e) {
	 * 		System.<jsf>err</jsf>.println(e.getMessage());  <jc>// Will display 200 lines of the output.</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <c>5</c>.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder debugOutputLines(int value) {
		return set(PARSER_debugOutputLines, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Parser listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define our parser listener.</jc>
	 * 	<jc>// Simply captures all unknown bean property events.</jc>
	 * 	<jk>public class</jk> MyParserListener <jk>extends</jk> ParserListener {
	 *
	 * 		<jc>// A simple property to store our events.</jc>
	 * 		<jk>public</jk> List&lt;String&gt; <jf>events</jf> = <jk>new</jk> LinkedList&lt;&gt;();
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> &lt;T&gt; <jk>void</jk> onUnknownBeanProperty(ParserSession session, ParserPipe pipe, String propertyName, Class&lt;T&gt; beanClass, T bean, <jk>int</jk> line, <jk>int</jk> col) {
	 * 			<jf>events</jf>.add(propertyName + <js>","</js> + line + <js>","</js> + col);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a parser using our listener.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.listener(MyParserListener.<jk>class</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Create a session object.</jc>
	 * 	<jc>// Needed because listeners are created per-session.</jc>
	 * 	<jk>try</jk> (ReaderParserSession s = p.createSession()) {
	 *
	 * 		<jc>// Parse some JSON object.</jc>
	 * 		MyBean myBean = s.parse(<js>"{...}"</js>, MyBean.<jk>class</jk>);
	 *
	 * 		<jc>// Get the listener.</jc>
	 * 		MyParserListener l = s.getListener(MyParserListener.<jk>class</jk>);
	 *
	 * 		<jc>// Dump the results to the console.</jc>
	 * 		SimpleJsonSerializer.<jsf>DEFAULT</jsf>.println(l.<jf>events</jf>);
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder listener(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Strict mode.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #strict()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public ParserBuilder strict(boolean value) {
		return set(PARSER_strict, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Strict mode.
	 *
	 * <p>
	 * When enabled, strict mode for the parser is enabled.
	 *
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 *
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser using strict mode.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.strict()
	 * 		.build();
	 *
	 * 	<jc>// Use it.</jc>
	 * 	<jk>try</jk> {
	 * 		String json = <js>"{unquotedAttr:'value'}"</js>;
	 * 		MyBean myBean = p.parse(json, MyBean.<jk>class</jk>);
	 * 	} <jk>catch</jk> (ParseException e) {
	 * 		<jsm>assertTrue</jsm>(e.getMessage().contains(<js>"Unquoted attribute detected."</js>);
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder strict() {
		return set(PARSER_strict, true);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Trim parsed strings.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #trimStrings()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public ParserBuilder trimStrings(boolean value) {
		return set(PARSER_trimStrings, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Trim parsed strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser with trim-strings enabled.</jc>
	 * 	ReaderParser p = JsonParser
	 * 		.<jsm>create</jsm>()
	 * 		.trimStrings()
	 * 		.build();
	 *
	 * 	<jc>// Use it.</jc>
	 * 	String json = <js>"{' foo ':' bar '}"</js>;
	 * 	Map&lt;String,String&gt; map = p.parse(json, HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Make sure strings are parsed.</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, map.get(<js>"foo"</js>));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder trimStrings() {
		return set(PARSER_trimStrings, true);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Unbuffered.
	 *
	 * <div class='warn'>
	 * 	<b>Deprecated</b> - Use {@link #unbuffered()}
	 * </div>
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	@ConfigurationProperty
	public ParserBuilder unbuffered(boolean value) {
		return set(PARSER_unbuffered, value);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Unbuffered.
	 *
	 * <p>
	 * When enabled, don't use internal buffering during parsing.
	 *
	 * <p>
	 * This is useful in cases when you want to parse the same input stream or reader multiple times
	 * because it may contain multiple independent POJOs to parse.
	 * <br>Buffering would cause the parser to read past the current POJO in the stream.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser using strict mode.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.unbuffered(<jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// If you're calling parse on the same input multiple times, use a session instead of the parser directly.</jc>
	 * 	<jc>// It's more efficient because we don't need to recalc the session settings again. </jc>
	 * 	ReaderParserSession s = p.createSession();
	 *
	 * 	<jc>// Read input with multiple POJOs</jc>
	 * 	Reader json = <jk>new</jk> StringReader(<js>"{foo:'bar'}{foo:'baz'}"</js>);
	 * 	MyBean myBean1 = s.parse(json, MyBean.<jk>class</jk>);
	 * 	MyBean myBean2 = s.parse(json, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This only allows for multi-input streams for the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link JsonParser}
	 * 			<li class='jc'>{@link UonParser}
	 * 		</ul>
	 * 		It has no effect on the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link MsgPackParser} - It already doesn't use buffering.
	 * 			<li class='jc'>{@link XmlParser}, {@link HtmlParser} - These use StAX which doesn't allow for more than one root element anyway.
	 * 			<li>RDF parsers - These read everything into an internal model before any parsing begins.
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public ParserBuilder unbuffered() {
		return set(PARSER_unbuffered, true);
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> ParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> ParserBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder propertyFilter(Class<?> on, Class<? extends org.apache.juneau.transform.PropertyFilter> value) {
		super.propertyFilter(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public ParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </CONFIGURATION-PROPERTIES>

	@Override /* Context */
	public Parser build() {
		return null;
	}
}