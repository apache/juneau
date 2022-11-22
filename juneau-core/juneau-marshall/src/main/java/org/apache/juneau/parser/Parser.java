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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Parent class for all Juneau parsers.
 *
 * <h5 class='topic'>Valid data conversions</h5>
 * <p>
 * Parsers can parse any parsable POJO types, as specified in the <a class="doclink" href="../../../../index.html#jm.PojoCategories">POJO Categories</a>.
 *
 * <p>
 * Some examples of conversions are shown below...
 * </p>
 * <table class='styled'>
 * 	<tr>
 * 		<th>Data type</th>
 * 		<th>Class type</th>
 * 		<th>JSON example</th>
 * 		<th>XML example</th>
 * 		<th>Class examples</th>
 * 	</tr>
 * 	<tr>
 * 		<td>object</td>
 * 		<td>Maps, Java beans</td>
 * 		<td class='code'>{name:<js>'John Smith'</js>,age:21}</td>
 * 		<td class='code'><xt>&lt;object&gt;
 * 	&lt;name</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;
 * 	&lt;age</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>21<xt>&lt;/age&gt;
 * &lt;/object&gt;</xt></td>
 * 		<td class='code'>HashMap, TreeMap&lt;String,Integer&gt;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>array</td>
 * 		<td>Collections, Java arrays</td>
 * 		<td class='code'>[1,2,3]</td>
 * 		<td class='code'><xt>&lt;array&gt;
 * 	&lt;number&gt;</xt>1<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>2<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>3<xt>&lt;/number&gt;
 * &lt;/array&gt;</xt></td>
 * 		<td class='code'>List&lt;Integer&gt;, <jk>int</jk>[], Float[], Set&lt;Person&gt;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>number</td>
 * 		<td>Numbers</td>
 * 		<td class='code'>123</td>
 * 		<td class='code'><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></td>
 * 		<td class='code'>Integer, Long, Float, <jk>int</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>boolean</td>
 * 		<td>Booleans</td>
 * 		<td class='code'><jk>true</jk></td>
 * 		<td class='code'><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></td>
 * 		<td class='code'>Boolean</td>
 * 	</tr>
 * 	<tr>
 * 		<td>string</td>
 * 		<td>CharSequences</td>
 * 		<td class='code'><js>'foobar'</js></td>
 * 		<td class='code'><xt>&lt;string&gt;</xt>foobar<xt>&lt;/string&gt;</xt></td>
 * 		<td class='code'>String, StringBuilder</td>
 * 	</tr>
 * </table>
 *
 * <p>
 * In addition, any class types with {@link ObjectSwap ObjectSwaps} associated with them on the registered
 * bean context can also be passed in.
 *
 * <p>
 * For example, if the {@link TemporalCalendarSwap} transform is used to generalize {@code Calendar} objects to {@code String}
 * objects.
 * When registered with this parser, you can construct {@code Calendar} objects from {@code Strings} using the
 * following syntax...
 * <p class='bjava'>
 * 	Calendar <jv>calendar</jv> = <jv>parser</jv>.parse(<js>"'Sun Mar 03 04:05:06 EST 2001'"</js>, GregorianCalendar.<jk>class</jk>);
 * </p>
 *
 * <p>
 * If <code>Object.<jk>class</jk></code> is specified as the target type, then the parser automatically determines the
 * data types and generates the following object types...
 * <table class='styled'>
 * 	<tr><th>JSON type</th><th>Class type</th></tr>
 * 	<tr><td>object</td><td>{@link JsonMap}</td></tr>
 * 	<tr><td>array</td><td>{@link JsonList}</td></tr>
 * 	<tr><td>number</td><td>{@link Number}<br>(depending on length and format, could be {@link Integer},
 * 		{@link Double}, {@link Float}, etc...)</td></tr>
 * 	<tr><td>boolean</td><td>{@link Boolean}</td></tr>
 * 	<tr><td>string</td><td>{@link String}</td></tr>
 * </table>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class Parser extends BeanContextable {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Represents no Parser.
	 */
	public static abstract class Null extends Parser {
		private Null(Builder builder) {
			super(builder);
		}
	}

	/**
	 * Instantiates a builder of the specified parser class.
	 *
	 * <p>
	 * Looks for a public static method called <c>create</c> that returns an object that can be passed into a public
	 * or protected constructor of the class.
	 *
	 * @param c The builder to create.
	 * @return A new builder.
	 */
	public static Builder createParserBuilder(Class<? extends Parser> c) {
		return (Builder)Context.createBuilder(c);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanContextable.Builder {

		boolean autoCloseStreams, strict, trimStrings, unbuffered;
		String consumes;
		int debugOutputLines;
		Class<? extends ParserListener> listener;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			autoCloseStreams = env("Parser.autoCloseStreams", false);
			strict = env("Parser.strict", false);
			trimStrings = env("Parser.trimStrings", false);
			unbuffered = env("Parser.unbuffered", false);
			debugOutputLines = env("Parser.debugOutputLines", 5);
			listener = null;
			consumes = null;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(Parser copyFrom) {
			super(copyFrom);
			autoCloseStreams = copyFrom.autoCloseStreams;
			strict = copyFrom.strict;
			trimStrings = copyFrom.trimStrings;
			unbuffered = copyFrom.unbuffered;
			debugOutputLines = copyFrom.debugOutputLines;
			listener = copyFrom.listener;
			consumes = copyFrom.consumes;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			autoCloseStreams = copyFrom.autoCloseStreams;
			strict = copyFrom.strict;
			trimStrings = copyFrom.trimStrings;
			unbuffered = copyFrom.unbuffered;
			debugOutputLines = copyFrom.debugOutputLines;
			listener = copyFrom.listener;
			consumes = copyFrom.consumes;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public Parser build() {
			return build(Parser.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				autoCloseStreams,
				strict,
				trimStrings,
				unbuffered,
				debugOutputLines,
				listener,
				consumes
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies the media type that this parser consumes.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder consumes(String value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Returns the current value for the 'consumes' property.
		 *
		 * @return The current value for the 'consumes' property.
		 */
		public String getConsumes() {
			return consumes;
		}

		/**
		 * Auto-close streams.
		 *
		 * <p>
		 * When enabled, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
		 * after parsing is complete.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser using strict mode.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.autoCloseStreams()
		 * 		.build();
		 *
		 * 	Reader <jv>myReader</jv> = <jk>new</jk> FileReader(<js>"/tmp/myfile.json"</js>);
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<jv>myReader</jv>, MyBean.<jk>class</jk>);
		 *
		 * 	<jsm>assertTrue</jsm>(<jv>myReader</jv>.isClosed());
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder autoCloseStreams() {
			return autoCloseStreams(true);
		}

		/**
		 * Same as {@link #autoCloseStreams()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder autoCloseStreams(boolean value) {
			autoCloseStreams = value;
			return this;
		}

		/**
		 * Debug output lines.
		 *
		 * <p>
		 * When parse errors occur, this specifies the number of lines of input before and after the
		 * error location to be printed as part of the exception message.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser whose exceptions print out 100 lines before and after the parse error location.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.debug()  <jc>// Enable debug mode to capture Reader contents as strings.</jc>
		 * 		.debugOuputLines(100)
		 * 		.build();
		 *
		 * 	Reader <jv>myReader</jv> = <jk>new</jk> FileReader(<js>"/tmp/mybadfile.json"</js>);
		 * 	<jk>try</jk> {
		 * 		<jv>parser</jv>.parse(<jv>myReader</jv>, Object.<jk>class</jk>);
		 * 	} <jk>catch</jk> (ParseException <jv>e</jv>) {
		 * 		System.<jsf>err</jsf>.println(<jv>e</jv>.getMessage());  <jc>// Will display 200 lines of the output.</jc>
		 * 	}
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is <c>5</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debugOutputLines(int value) {
			debugOutputLines = value;
			return this;
		}

		/**
		 * Parser listener.
		 *
		 * <p>
		 * Class used to listen for errors and warnings that occur during parsing.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Define our parser listener.</jc>
		 * 	<jc>// Simply captures all unknown bean property events.</jc>
		 * 	<jk>public class</jk> MyParserListener <jk>extends</jk> ParserListener {
		 *
		 * 		<jc>// A simple property to store our events.</jc>
		 * 		<jk>public</jk> List&lt;String&gt; <jf>events</jf> = <jk>new</jk> LinkedList&lt;&gt;();
		 *
		 * 		<ja>@Override</ja>
		 * 		<jk>public</jk> &lt;T&gt; <jk>void</jk> onUnknownBeanProperty(ParserSession <jv>session</jv>, String <jv>propertyName</jv>, Class&lt;T&gt; <jv>beanClass</jv>, T <jv>bean</jv>) {
		 * 			Position <jv>position</jv> = <jv>parser</jv>.getPosition();
		 * 			<jf>events</jf>.add(<jv>propertyName</jv> + <js>","</js> + <jv>position</jv>.getLine() + <js>","</js> + <jv>position</jv>.getColumn());
		 * 		}
		 * 	}
		 *
		 * 	<jc>// Create a parser using our listener.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.listener(MyParserListener.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	<jc>// Create a session object.</jc>
		 * 	<jc>// Needed because listeners are created per-session.</jc>
		 * 	<jk>try</jk> (ReaderParserSession <jv>session</jv> = <jv>parser</jv>.createSession()) {
		 *
		 * 		<jc>// Parse some JSON object.</jc>
		 * 		MyBean <jv>myBean</jv> = <jv>session</jv>.parse(<js>"{...}"</js>, MyBean.<jk>class</jk>);
		 *
		 * 		<jc>// Get the listener.</jc>
		 * 		MyParserListener <jv>listener</jv> = <jv>session</jv>.getListener(MyParserListener.<jk>class</jk>);
		 *
		 * 		<jc>// Dump the results to the console.</jc>
		 * 		Json5.<jsf>DEFAULT</jsf>.println(<jv>listener</jv>.<jf>events</jf>);
		 * 	}
		 * </p>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@FluentSetter
		public Builder listener(Class<? extends ParserListener> value) {
			listener = value;
			return this;
		}

		/**
		 * Strict mode.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a parser using strict mode.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.strict()
		 * 		.build();
		 *
		 * 	<jc>// Use it.</jc>
		 * 	<jk>try</jk> {
		 * 		String <jv>json</jv> = <js>"{unquotedAttr:'value'}"</js>;
		 * 		<jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * 	} <jk>catch</jk> (ParseException <jv>e</jv>) {
		 * 		<jsm>assertTrue</jsm>(<jv>e</jv>.getMessage().contains(<js>"Unquoted attribute detected."</js>);
		 * 	}
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder strict() {
			return strict(true);
		}

		/**
		 * Same as {@link #strict()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder strict(boolean value) {
			strict = value;
			return this;
		}

		/**
		 * Trim parsed strings.
		 *
		 * <p>
		 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being added to
		 * the POJO.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser with trim-strings enabled.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.trimStrings()
		 * 		.build();
		 *
		 * 	<jc>// Use it.</jc>
		 * 	String <jv>json</jv> = <js>"{' foo ':' bar '}"</js>;
		 * 	Map&lt;String,String&gt; <jv>myMap</jv> = <jv>parser</jv>.parse(<jv>json</jv>, HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
		 *
		 * 	<jc>// Make sure strings are parsed.</jc>
		 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, <jv>myMap</jv>.get(<js>"foo"</js>));
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStrings() {
			return trimStrings(true);
		}

		/**
		 * Same as {@link #trimStrings()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder trimStrings(boolean value) {
			trimStrings = value;
			return this;
		}

		/**
		 * Unbuffered.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a parser using strict mode.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser.
		 * 		.<jsm>create</jsm>()
		 * 		.unbuffered(<jk>true</jk>)
		 * 		.build();
		 *
		 * 	<jc>// If you're calling parse on the same input multiple times, use a session instead of the parser directly.</jc>
		 * 	<jc>// It's more efficient because we don't need to recalc the session settings again. </jc>
		 * 	ReaderParserSession <jv>session</jv> = <jv>parser</jv>.createSession();
		 *
		 * 	<jc>// Read input with multiple POJOs</jc>
		 * 	Reader <jv>json</jv> = <jk>new</jk> StringReader(<js>"{foo:'bar'}{foo:'baz'}"</js>);
		 * 	MyBean <jv>myBean1</jv> = <jv>session</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * 	MyBean <jv>myBean2</jv> = <jv>session</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder unbuffered() {
			return unbuffered(true);
		}

		/**
		 * Same as {@link #unbuffered()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder unbuffered(boolean value) {
			unbuffered = value;
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

		// </FluentSetters>
	}
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean trimStrings, strict, autoCloseStreams, unbuffered;
	final int debugOutputLines;
	final String consumes;
	final Class<? extends ParserListener> listener;

	/** General parser properties currently set on this parser. */
	private final MediaType[] consumesArray;

	/**
	 * Constructor.
	 *
	 * @param builder The builder this object.
	 */
	protected Parser(Builder builder) {
		super(builder);

		consumes = builder.consumes;
		trimStrings = builder.trimStrings;
		strict = builder.strict;
		autoCloseStreams = builder.autoCloseStreams;
		debugOutputLines = builder.debugOutputLines;
		unbuffered = builder.unbuffered;
		listener = builder.listener;

		String[] _consumes = split(consumes != null ? consumes : "");
		this.consumesArray = new MediaType[_consumes.length];
		for (int i = 0; i < _consumes.length; i++) {
			this.consumesArray[i] = MediaType.of(_consumes[i]);
		}
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 *
	 * @return <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 */
	public boolean isReaderParser() {
		return true;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parses input into the specified object type.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	ReaderParser <jv>parser</jv> = JsonParser.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List <jv>list1</jv> = <jv>parser</jv>.parse(<jv>json</jv>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List <jv>list2</jv> = <jv>parser</jv>.parse(<jv>json</jv>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List <jv>list3</jv> = <jv>parser</jv>.parse(<jv>json</jv>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map <jv>map1</jv> = <jv>parser</jv>.parse(<jv>json</jv>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map <jv>map2</jv> = <jv>parser</jv>.parse(<jv>json</jv>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Use the {@link #parse(Object, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input
	 * 	The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link ReaderParser.Builder#fileCharset(Charset)} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T parse(Object input, Type type, Type...args) throws ParseException, IOException {
		return getSession().parse(input, type, args);
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} but since it's a {@link String} input doesn't throw an {@link IOException}.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public final <T> T parse(String input, Type type, Type...args) throws ParseException {
		return getSession().parse(input, type, args);
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	ReaderParser <jv>parser</jv> = JsonParser.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String <jv>string</jv> = <jv>parser</jv>.parse(<jv>json</jv>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] <jv>beanArray</jv> = <jv>parser</jv>.parse(<jv>json</jv>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List <jv>list</jv> = <jv>parser</jv>.parse(<jv>json</jv>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>parser</jv>.parse(<jv>json</jv>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final <T> T parse(Object input, Class<T> type) throws ParseException, IOException {
		return getSession().parse(input, type);
	}

	/**
	 * Same as {@link #parse(Object, Class)} but since it's a {@link String} input doesn't throw an {@link IOException}.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public final <T> T parse(String input, Class<T> type) throws ParseException {
		return getSession().parse(input, type);
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except the type has already been converted into a {@link ClassMeta}
	 * object.
	 *
	 * <p>
	 * This is mostly an internal method used by the framework.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException, IOException {
		return getSession().parse(input, type);
	}

	/**
	 * Same as {@link #parse(Object, ClassMeta)} but since it's a {@link String} input doesn't throw an {@link IOException}.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 */
	public final <T> T parse(String input, ClassMeta<T> type) throws ParseException {
		return getSession().parse(input, type);
	}

	@Override /* Context */
	public ParserSession.Builder createSession() {
		return ParserSession.create(this);
	}

	@Override /* Context */
	public ParserSession getSession() {
		return createSession().build();
	}

	/**
	 * Workhorse method.
	 *
	 * <p>
	 * Subclasses are expected to either implement this method or {@link ParserSession#doParse(ParserPipe, ClassMeta)}.
	 *
	 * @param session The current session.
	 * @param pipe Where to get the input from.
	 * @param type
	 * 	The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <c>String</c>, <c>Number</c>,
	 * 	<c>JsonMap</c>, etc...
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException Malformed input encountered.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T doParse(ParserSession session, ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		throw new UnsupportedOperationException();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Optional methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parses the contents of the specified reader and loads the results into the specified map.
	 *
	 * <p>
	 * Reader must contain something that serializes to a map (such as text containing a JSON object).
	 *
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The various character-based constructors in {@link JsonMap} (e.g.
	 * 		{@link JsonMap#JsonMap(CharSequence,Parser)}).
	 * </ul>
	 *
	 * @param <K> The key class type.
	 * @param <V> The value class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException Malformed input encountered.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <K,V> Map<K,V> parseIntoMap(Object input, Map<K,V> m, Type keyType, Type valueType) throws ParseException {
		return getSession().parseIntoMap(input, m, keyType, valueType);
	}

	/**
	 * Parses the contents of the specified reader and loads the results into the specified collection.
	 *
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The various character-based constructors in {@link JsonList} (e.g.
	 * 		{@link JsonList#JsonList(CharSequence,Parser)}.
	 * </ul>
	 *
	 * @param <E> The element class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException Malformed input encountered.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <E> Collection<E> parseIntoCollection(Object input, Collection<E> c, Type elementType) throws ParseException {
		return getSession().parseIntoCollection(input, c, elementType);
	}

	/**
	 * Parses the specified array input with each entry in the object defined by the {@code argTypes}
	 * argument.
	 *
	 * <p>
	 * Used for converting arrays (e.g. <js>"[arg1,arg2,...]"</js>) into an {@code Object[]} that can be passed
	 * to the {@code Method.invoke(target, args)} method.
	 *
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Used to parse argument strings in the {@link ObjectIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 *
	 * @param input The input.  Subclasses can support different input types.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException Malformed input encountered.
	 */
	public final Object[] parseArgs(Object input, Type[] argTypes) throws ParseException {
		if (argTypes == null || argTypes.length == 0)
			return new Object[0];
		return getSession().parseArgs(input, argTypes);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the values passed to the <c>consumes</c> constructor parameter.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final List<MediaType> getMediaTypes() {
		return ulist(consumesArray);
	}

	/**
	 * Returns the first media type handled based on the values passed to the <c>consumes</c> constructor parameter.
	 *
	 * @return The media type.
	 */
	public final MediaType getPrimaryMediaType() {
		return consumesArray == null || consumesArray.length == 0 ? null : consumesArray[0];
	}

	/**
	 * Returns <jk>true</jk> if this parser can handle the specified content type.
	 *
	 * @param contentType The content type to test.
	 * @return <jk>true</jk> if this parser can handle the specified content type.
	 */
	public boolean canHandle(String contentType) {
		if (contentType != null)
			for (MediaType mt : getMediaTypes())
				if (contentType.equals(mt.toString()))
					return true;
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Auto-close streams.
	 *
	 * @see Parser.Builder#autoCloseStreams()
	 * @return
	 * 	<jk>true</jk> if <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * 	after parsing is complete.
	 */
	protected final boolean isAutoCloseStreams() {
		return autoCloseStreams;
	}

	/**
	 * Debug output lines.
	 *
	 * @see Parser.Builder#debugOutputLines(int)
	 * @return
	 * 	The number of lines of input before and after the error location to be printed as part of the exception message.
	 */
	protected final int getDebugOutputLines() {
		return debugOutputLines;
	}

	/**
	 * Parser listener.
	 *
	 * @see Parser.Builder#listener(Class)
	 * @return
	 * 	Class used to listen for errors and warnings that occur during parsing.
	 */
	protected final Class<? extends ParserListener> getListener() {
		return listener;
	}

	/**
	 * Strict mode.
	 *
	 * @see Parser.Builder#strict()
	 * @return
	 * 	<jk>true</jk> if strict mode for the parser is enabled.
	 */
	protected final boolean isStrict() {
		return strict;
	}

	/**
	 * Trim parsed strings.
	 *
	 * @see Parser.Builder#trimStrings()
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * 	the POJO.
	 */
	protected final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Unbuffered.
	 *
	 * @see Parser.Builder#unbuffered()
	 * @return
	 * 	<jk>true</jk> if parsers don't use internal buffering during parsing.
	 */
	protected final boolean isUnbuffered() {
		return unbuffered;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("autoCloseStreams", autoCloseStreams)
			.append("debugOutputLines", debugOutputLines)
			.append("listener", listener)
			.append("strict", strict)
			.append("trimStrings", trimStrings)
			.append("unbuffered", unbuffered);
	}
}
