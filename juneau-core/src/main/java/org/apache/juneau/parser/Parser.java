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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all Juneau parsers.
 *
 * <h6 class='topic'>@Consumes annotation</h6>
 * <p>
 * The media types that this parser can handle is specified through the {@link Consumes @Consumes} annotation.
 * <p>
 * However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()} method.
 *
 * <a id='ValidDataConversions'></a><h6 class='topic'>Valid data conversions</h6>
 * Parsers can parse any parsable POJO types, as specified in the <a class="doclink" href="../../../../overview-summary.html#Core.PojoCategories">POJO Categories</a>.
 * <p>
 * Some examples of conversions are shown below...
 * </p>
 * 	<table class='styled'>
 * 		<tr>
 * 			<th>Data type</th>
 * 			<th>Class type</th>
 * 			<th>JSON example</th>
 * 			<th>XML example</th>
 * 			<th>Class examples</th>
 * 		</tr>
 * 		<tr>
 * 			<td>object</td>
 * 			<td>Maps, Java beans</td>
 * 			<td class='code'>{name:<js>'John Smith'</js>,age:21}</td>
 * 			<td class='code'><xt>&lt;object&gt;
 * 	&lt;name</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;
 * 	&lt;age</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>21<xt>&lt;/age&gt;
 * &lt;/object&gt;</xt></td>
 * 			<td class='code'>HashMap, TreeMap&lt;String,Integer&gt;</td>
 * 		</tr>
 * 		<tr>
 * 			<td>array</td>
 * 			<td>Collections, Java arrays</td>
 * 			<td class='code'>[1,2,3]</td>
 * 			<td class='code'><xt>&lt;array&gt;
 * 	&lt;number&gt;</xt>1<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>2<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>3<xt>&lt;/number&gt;
 * &lt;/array&gt;</xt></td>
 * 			<td class='code'>List&lt;Integer&gt;, <jk>int</jk>[], Float[], Set&lt;Person&gt;</td>
 * 		</tr>
 * 		<tr>
 * 			<td>number</td>
 * 			<td>Numbers</td>
 * 			<td class='code'>123</td>
 * 			<td class='code'><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></td>
 * 			<td class='code'>Integer, Long, Float, <jk>int</jk></td>
 * 		</tr>
 * 		<tr>
 * 			<td>boolean</td>
 * 			<td>Booleans</td>
 * 			<td class='code'><jk>true</jk></td>
 * 			<td class='code'><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></td>
 * 			<td class='code'>Boolean</td>
 * 		</tr>
 * 		<tr>
 * 			<td>string</td>
 * 			<td>CharSequences</td>
 * 			<td class='code'><js>'foobar'</js></td>
 * 			<td class='code'><xt>&lt;string&gt;</xt>foobar<xt>&lt;/string&gt;</xt></td>
 * 			<td class='code'>String, StringBuilder</td>
 * 		</tr>
 * 	</table>
 * <p>
 * In addition, any class types with {@link PojoSwap PojoSwaps} associated with them on the registered
 * 	{@link #getBeanContext() beanContext} can also be passed in.
 * <p>
 * For example, if the {@link CalendarSwap} transform is used to generalize {@code Calendar} objects to {@code String} objects.  When registered
 * 	with this parser, you can construct {@code Calendar} objects from {@code Strings} using the following syntax...
 * <p class='bcode'>
 * 	Calendar c = parser.parse(<js>"'Sun Mar 03 04:05:06 EST 2001'"</js>, GregorianCalendar.<jk>class</jk>);
 * <p>
 * If <code>Object.<jk>class</jk></code> is specified as the target type, then the parser
 * 	automatically determines the data types and generates the following object types...
 * </p>
 * <table class='styled'>
 * 	<tr><th>JSON type</th><th>Class type</th></tr>
 * 	<tr><td>object</td><td>{@link ObjectMap}</td></tr>
 * 	<tr><td>array</td><td>{@link ObjectList}</td></tr>
 * 	<tr><td>number</td><td>{@link Number} <br>(depending on length and format, could be {@link Integer}, {@link Double}, {@link Float}, etc...)</td></tr>
 * 	<tr><td>boolean</td><td>{@link Boolean}</td></tr>
 * 	<tr><td>string</td><td>{@link String}</td></tr>
 * </table>
 *
 * <a id='SupportedTypes'></a><h6 class='topic'>Supported types</h6>
 * <p>
 * Several of the methods below take {@link Type} parameters to identify the type of
 * 	object to create.  Any of the following types can be passed in to these methods...
 * </p>
 * <ul>
 * 	<li>{@link ClassMeta}
 * 	<li>{@link Class}
 * 	<li>{@link ParameterizedType}
 * 	<li>{@link GenericArrayType}
 * </ul>
 * <p>
 * However, {@code ParameterizedTypes} and {@code GenericArrayTypes} should not contain
 * 	{@link WildcardType WildcardTypes} or {@link TypeVariable TypeVariables}.
 * <p>
 * Passing in <jk>null</jk> or <code>Object.<jk>class</jk></code> typically signifies that it's up to the parser
 * 	to determine what object type is being parsed parsed based on the rules above.
 */
public abstract class Parser extends CoreObject {

	/** General parser properties currently set on this parser. */
	private final MediaType[] mediaTypes;
	private final ParserContext ctx;

	// Hidden constructor to force subclass from InputStreamParser or ReaderParser.
	Parser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(ParserContext.class);

		Consumes c = getAnnotation(Consumes.class, getClass());
		if (c == null)
			throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Consumes annotation", getClass().getName()));

		String[] mt = split(c.value());
		this.mediaTypes = new MediaType[mt.length];
		for (int i = 0; i < mt.length; i++) {
			mediaTypes[i] = MediaType.forString(mt[i]);
		}
	}

	@Override /* CoreObject */
	public ParserBuilder builder() {
		return new ParserBuilder(propertyStore);
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Workhorse method.  Subclasses are expected to implement this method.
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object, Locale, TimeZone, MediaType)}.
	 * If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
	 * @param type The class type of the object to create.
	 * If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected abstract <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception;

	/**
	 * Returns <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 *
	 * @return <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 */
	public abstract boolean isReaderParser();

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Entry point for all parsing calls.
	 * <p>
	 * Calls the {@link #doParse(ParserSession, ClassMeta)} implementation class and catches/rewraps any exceptions thrown.
	 * @param session The runtime session returned by {@link #createSession(Object, ObjectMap, Method, Object, Locale, TimeZone, MediaType)}.
	 * @param type The class type of the object to create.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parseSession(ParserSession session, ClassMeta<T> type) throws ParseException {
		try {
			if (type.isVoid())
				return null;
			return doParse(session, type);
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(session, "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(session, "I/O exception occurred.  exception={0}, message={1}.", e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} catch (Exception e) {
			throw new ParseException(session, "Exception occurred.  exception={0}, message={1}.", e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} finally {
			session.close();
		}
	}

	/**
	 * Parses input into the specified object type.
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Use the {@link #parse(Object, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by {@link ParserContext#PARSER_inputStreamCharset} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by {@link ParserContext#PARSER_inputStreamCharset} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by {@link ParserContext#PARSER_fileCharset} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T parse(Object input, Type type, Type...args) throws ParseException {
		ParserSession session = createSession(input);
		return (T)parseSession(session, session.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except optimized for a non-parameterized class.
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = p.parse(json, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = p.parse(json, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] ba = p.parse(json, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, Class<T> type) throws ParseException {
		ParserSession session = createSession(input);
		return parseSession(session, session.getClassMeta(type));
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except the type has already been converted into a {@link ClassMeta} object.
	 * <p>
	 * This is mostly an internal method used by the framework.
	 *
	 * @param <T> The class type of the object being created.
	 * @param input The input.
	 * See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException {
		return parseSession(createSession(input), type);
	}

	/**
	 * Create the session object that will be passed in to the parse method.
	 * <p>
	 * It's up to implementers to decide what the session object looks like, although typically
	 * 	it's going to be a subclass of {@link ParserSession}.
	 *
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param op Optional additional properties.
	 * @param javaMethod Java method that invoked this parser.
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @return The new session.
	 */
	public ParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new ParserSession(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	/**
	 * Create a basic session object without overriding properties or specifying <code>javaMethod</code>.
	 * <p>
	 * Equivalent to calling <code>createSession(<jk>null</jk>, <jk>null</jk>)</code>.
	 *
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @return The new context.
	 */
	protected final ParserSession createSession(Object input) {
		return createSession(input, null, null, null, null, null, getPrimaryMediaType());
	}


	//--------------------------------------------------------------------------------
	// Optional methods
	//--------------------------------------------------------------------------------

	/**
	 * Parses the contents of the specified reader and loads the results into the specified map.
	 * <p>
	 * Reader must contain something that serializes to a map (such as text containing a JSON object).
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>The various character-based constructors in {@link ObjectMap} (e.g. {@link ObjectMap#ObjectMap(CharSequence,Parser)}).
	 * </ul>
	 *
	 * @param <K> The key class type.
	 * @param <V> The value class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.<br>
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.<br>
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <K,V> Map<K,V> parseIntoMap(Object input, Map<K,V> m, Type keyType, Type valueType) throws ParseException {
		ParserSession session = createSession(input);
		try {
			return doParseIntoMap(session, m, keyType, valueType);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}

	/**
	 * Implementation method.
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object, Locale, TimeZone, MediaType)}.
	 * If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.<br>
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.<br>
	 *
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Parses the contents of the specified reader and loads the results into the specified collection.
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>The various character-based constructors in {@link ObjectList} (e.g. {@link ObjectList#ObjectList(CharSequence,Parser)}.
	 * </ul>
	 *
	 * @param <E> The element class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <E> Collection<E> parseIntoCollection(Object input, Collection<E> c, Type elementType) throws ParseException {
		ParserSession session = createSession(input);
		try {
			return doParseIntoCollection(session, c, elementType);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}

	/**
	 * Implementation method.
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object, Locale, TimeZone, MediaType)}.
	 * If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 *
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Parses the specified array input with each entry in the object defined by the {@code argTypes}
	 * argument.
	 * <p>
	 * Used for converting arrays (e.g. <js>"[arg1,arg2,...]"</js>) into an {@code Object[]} that can be passed
	 * 	to the {@code Method.invoke(target, args)} method.
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>Used to parse argument strings in the {@link PojoIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 *
	 * @param input The input.  Subclasses can support different input types.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final Object[] parseArgs(Object input, Type[] argTypes) throws ParseException {
		if (argTypes == null || argTypes.length == 0)
			return new Object[0];
		ParserSession session = createSession(input);
		try {
			return doParse(session, session.getArgsClassMeta(argTypes));
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Converts the specified string to the specified type.
	 *
	 * @param session The session object.
	 * @param outer The outer object if we're converting to an inner object that needs to be created within the context of an outer object.
	 * @param s The string to convert.
	 * @param type The class type to convert the string to.
	 * @return The string converted as an object of the specified type.
	 * @throws Exception If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @param <T> The class type to convert the string to.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T> T convertAttrToType(ParserSession session, Object outer, String s, ClassMeta<T> type) throws Exception {
		if (s == null)
			return null;

		if (type == null)
			type = (ClassMeta<T>)object();
		PojoSwap transform = type.getPojoSwap();
		ClassMeta<?> sType = type.getSerializedClassMeta();

		Object o = s;
		if (sType.isChar())
			o = s.charAt(0);
		else if (sType.isNumber())
			if (type.canCreateNewInstanceFromNumber(outer))
				o = type.newInstanceFromNumber(session, outer, parseNumber(s, type.getNewInstanceFromNumberClass()));
			else
				o = parseNumber(s, (Class<? extends Number>)sType.getInnerClass());
		else if (sType.isBoolean())
			o = Boolean.parseBoolean(s);
		else if (! (sType.isCharSequence() || sType.isObject())) {
			if (sType.canCreateNewInstanceFromString(outer))
				o = sType.newInstanceFromString(outer, s);
			else
				throw new ParseException(session, "Invalid conversion from string to class ''{0}''", type);
		}

		if (transform != null)
			o = transform.unswap(session, o, type);

		return (T)o;
	}

	/**
	 * Convenience method for calling the {@link ParentProperty @ParentProperty} method on
	 * the specified object if it exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param parent The parent to set.
	 * @throws Exception
	 */
	protected void setParent(ClassMeta<?> cm, Object o, Object parent) throws Exception {
		Setter m = cm.getParentProperty();
		if (m != null)
			m.set(o, parent);
	}

	/**
	 * Convenience method for calling the {@link NameProperty @NameProperty} method on
	 * the specified object if it exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param name The name to set.
	 * @throws Exception
	 */
	protected void setName(ClassMeta<?> cm, Object o, Object name) throws Exception {
		if (cm != null) {
			Setter m = cm.getNameProperty();
			if (m != null)
				m.set(o, name);
		}
	}

	/**
	 * Returns the media types handled based on the value of the {@link Consumes} annotation on the parser class.
	 * <p>
	 * This method can be overridden by subclasses to determine the media types programatically.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public MediaType[] getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * Returns the first media type specified on this parser via the {@link Consumes} annotation.
	 *
	 * @return The media type.
	 */
	public MediaType getPrimaryMediaType() {
		return mediaTypes == null || mediaTypes.length == 0 ? null : mediaTypes[0];
	}
}
