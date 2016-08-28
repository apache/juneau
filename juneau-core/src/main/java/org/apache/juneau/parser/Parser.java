/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.parser;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all Juneau parsers.
 *
 *
 * <h6 class='topic'>@Consumes annotation</h6>
 * <p>
 * 	The media types that this parser can handle is specified through the {@link Consumes @Consumes} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()} method.
 *
 *
 * <a id='ValidDataConversions'></a><h6 class='topic'>Valid data conversions</h6>
 * 	Parsers can parse any parsable POJO types, as specified in the <a class='doclink' href='../package-summary.html#PojoCategories'>POJO Categories</a>.
 * <p>
 * 	Some examples of conversions are shown below...
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
 * 	In addition, any class types with {@link PojoSwap PojoSwaps} associated with them on the registered
 * 		{@link #getBeanContext() beanContext} can also be passed in.
 * <p>
 * 	For example, if the {@link CalendarSwap} transform is used to generalize {@code Calendar} objects to {@code String} objects.  When registered
 * 	with this parser, you can construct {@code Calendar} objects from {@code Strings} using the following syntax...
 * <p class='bcode'>
 * 	Calendar c = parser.parse(<js>"'Sun Mar 03 04:05:06 EST 2001'"</js>, GregorianCalendar.<jk>class</jk>);
 * <p>
 * 	If <code>Object.<jk>class</jk></code> is specified as the target type, then the parser
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
 *
 * <a id='SupportedTypes'></a><h6 class='topic'>Supported types</h6>
 * <p>
 * 	Several of the methods below take {@link Type} parameters to identify the type of
 * 		object to create.  Any of the following types can be passed in to these methods...
 * </p>
 * <ul>
 * 	<li>{@link ClassMeta}
 * 	<li>{@link Class}
 * 	<li>{@link ParameterizedType}
 * 	<li>{@link GenericArrayType}
 * </ul>
 * <p>
 * 	However, {@code ParameterizedTypes} and {@code GenericArrayTypes} should not contain
 * 		{@link WildcardType WildcardTypes} or {@link TypeVariable TypeVariables}.
 * <p>
 * 	Passing in <jk>null</jk> or <code>Object.<jk>class</jk></code> typically signifies that it's up to the parser
 * 	to determine what object type is being parsed parsed based on the rules above.

 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public abstract class Parser extends CoreApi {

	/** General serializer properties currently set on this serializer. */
	private final List<ParserListener> listeners = new LinkedList<ParserListener>();
	private final String[] mediaTypes;
	private final MediaRange[] mediaRanges;

	// Hidden constructor to force subclass from InputStreamParser or ReaderParser.
	Parser() {
		Consumes c = ReflectionUtils.getAnnotation(Consumes.class, getClass());
		if (c == null)
			throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Consumes annotation", getClass().getName()));
		this.mediaTypes = c.value();
		for (int i = 0; i < mediaTypes.length; i++) {
			mediaTypes[i] = mediaTypes[i].toLowerCase(Locale.ENGLISH);
		}

		List<MediaRange> l = new LinkedList<MediaRange>();
		for (int i = 0; i < mediaTypes.length; i++)
			l.addAll(Arrays.asList(MediaRange.parse(mediaTypes[i])));
		mediaRanges = l.toArray(new MediaRange[l.size()]);
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Workhorse method.  Subclasses are expected to implement this method.
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 *
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
	 * Parses the content of the reader and creates an object of the specified type.
	 * @param session The runtime session returned by {@link #createSession(Object, ObjectMap, Method, Object)}.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 *
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(ParserSession session, ClassMeta<T> type) throws ParseException {
		try {
			return doParse(session, type);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}

	/**
	 * Parses the content of the reader and creates an object of the specified type.
	 * <p>
	 * Equivalent to calling <code>parser.parse(in, type, <jk>null</jk>);</code>
	 *
	 * @param input The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException {
		ParserSession session = createSession(input);
		return parse(session, type);
	}

	/**
	 * Parses input into the specified object type.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	MyBean b = p.parse(json, MyBean.<jk>class</jk>);
	 * 		</p>
	 * 		<p>
	 * 		This method equivalent to the following code:
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;MyBean&gt; cm = p.getBeanContext().getClassMeta(MyBean.<jk>class</jk>);
	 * 	MyBean b = p.parse(json, cm, <jk>null</jk>);
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param type The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, Class<T> type) throws ParseException {
		ClassMeta<T> cm = getBeanContext().getClassMeta(type);
		return parse(input, cm);
	}

	/**
	 * Parses input into a map with specified key and value types.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	Map&lt;String,MyBean&gt; m = p.parseMap(json, LinkedHashMap.<jk>class</jk>, String.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 		</p>
	 * 		<p>
	 * 			A simpler approach is often to just extend the map class you want and just use the normal {@link #parse(Object, Class)} method:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<jk>public static class</jk> MyMap <jk>extends</jk> LinkedHashMap&lt;String,MyBean&gt; {}
	 *
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	Map&lt;String,MyBean&gt; m = p.parse(json, MyMap.<jk>class</jk>);
	 * 		</p>
	 * 		<p>
	 * 			This method equivalent to the following code:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;Map&lt;String,MyBean&gt;&gt; cm = p.getBeanContext().getMapClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	Map&ltString,MyBean&gt; m = p.parse(json, cm, <jk>null</jk>);
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param mapClass The map class type.
	 * @param keyClass The key class type.
	 * @param valueClass The value class type.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <K,V,T extends Map<K,V>> T parseMap(Object input, Class<T> mapClass, Class<K> keyClass, Class<V> valueClass) throws ParseException {
		ClassMeta<T> cm = getBeanContext().getMapClassMeta(mapClass, keyClass, valueClass);
		return parse(input, cm);
	}

	/**
	 * Parses input into a collection with a specified element type.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	List&lt;MyBean&gt; l = p.parseCollection(json, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 		</p>
	 * 		<p>
	 * 			A simpler approach is often to just extend the collection class you want and just use the normal {@link #parse(Object, Class)} method:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<jk>public static class</jk> MyBeanCollection <jk>extends</jk> LinkedList&lt;MyBean&gt; {}
	 *
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	List&lt;MyBean&gt; l = p.parse(json, MyBeanCollection.<jk>class</jk>);
	 * 		</p>
	 * 		<p>
	 * 			This method equivalent to the following code:
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;List&lt;MyBean&gt;&gt; cm = p.getBeanContext().getCollectionClassMeta(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	List&lt;MyBean&gt; l = p.parse(json, cm, <jk>null</jk>);
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param collectionClass The collection class type.
	 * @param entryClass The class type of entries in the collection.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <E,T extends Collection<E>> T parseCollection(Object input, Class<T> collectionClass, Class<E> entryClass) throws ParseException, IOException {
		ClassMeta<T> cm = getBeanContext().getCollectionClassMeta(collectionClass, entryClass);
		return parse(input, cm);
	}

	/**
	 * Create the session object that will be passed in to the parse method.
	 * <p>
	 * 	It's up to implementers to decide what the session object looks like, although typically
	 * 	it's going to be a subclass of {@link ParserSession}.
	 *
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param properties Optional additional properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @return The new context.
	 */
	public ParserSession createSession(Object input, ObjectMap properties, Method javaMethod, Object outer) {
		return new ParserSession(getContext(ParserContext.class), getBeanContext(), input, properties, javaMethod, outer);
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
		return createSession(input, null, null, null);
	}

	//--------------------------------------------------------------------------------
	// Optional methods
	//--------------------------------------------------------------------------------

	/**
	 * Parses the contents of the specified reader and loads the results into the specified map.
	 * <p>
	 * 	Reader must contain something that serializes to a map (such as text containing a JSON object).
	 * <p>
	 * 	Used in the following locations:
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
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
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
	 * 	Used in the following locations:
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
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
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
	 * 	Used for converting arrays (e.g. <js>"[arg1,arg2,...]"</js>) into an {@code Object[]} that can be passed
	 * 	to the {@code Method.invoke(target, args)} method.
	 * <p>
	 * 	Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>Used to parse argument strings in the {@link PojoIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 *
	 * @param input The input.  Subclasses can support different input types.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final Object[] parseArgs(Object input, ClassMeta<?>[] argTypes) throws ParseException {
		if (argTypes == null || argTypes.length == 0)
			return new Object[0];
		ParserSession session = createSession(input);
		try {
			return doParseArgs(session, argTypes);
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
	 * @param session The runtime session object returned by {@link #createSession(Object, ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createSession(Object)}.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 *
	 * @return An array of parsed objects.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Adds a {@link ParserListener} to this parser to listen for parse events.
	 *
	 * @param listener The listener to associate with this parser.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public Parser addListener(ParserListener listener) throws LockedException {
		checkLock();
		this.listeners.add(listener);
		return this;
	}

	/**
	 * Returns the current parser listeners associated with this parser.
	 *
	 * @return The current list of parser listeners.
	 */
	public List<ParserListener> getListeners() {
		return listeners;
	}

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
		ClassMeta<?> gType = type.getSerializedClassMeta();

		Object o = s;
		if (gType.isChar())
			o = s.charAt(0);
		else if (gType.isNumber())
			if (type.canCreateNewInstanceFromNumber(outer))
				o = type.newInstanceFromNumber(outer, parseNumber(s, type.getNewInstanceFromNumberClass()));
			else
				o = parseNumber(s, (Class<? extends Number>)gType.getInnerClass());
		else if (gType.isBoolean())
			o = Boolean.parseBoolean(s);
		else if (! (gType.isCharSequence() || gType.isObject())) {
			if (gType.canCreateNewInstanceFromString(outer))
				o = gType.newInstanceFromString(outer, s);
			else
				throw new ParseException(session, "Invalid conversion from string to class ''{0}''", type);
		}

		if (transform != null)
			o = transform.unswap(o, type, session.getBeanContext());

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
		Method m = cm.getParentProperty();
		if (m != null)
			m.invoke(o, parent);
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
			Method m = cm.getNameProperty();
			if (m != null)
				m.invoke(o, name);
		}
	}

	/**
	 * Method that gets called when an unknown bean property name is encountered.
	 *
	 * @param session The parser session.
	 * @param propertyName The unknown bean property name.
	 * @param beanMap The bean that doesn't have the expected property.
	 * @param line The line number where the property was found.  <code>-1</code> if line numbers are not available.
	 * @param col The column number where the property was found.  <code>-1</code> if column numbers are not available.
	 * @throws ParseException Automatically thrown if {@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting
	 * 	on this parser is <jk>false</jk>
	 * @param <T> The class type of the bean map that doesn't have the expected property.
	 */
	protected <T> void onUnknownProperty(ParserSession session, String propertyName, BeanMap<T> beanMap, int line, int col) throws ParseException {
		if (propertyName.equals("uri") || propertyName.equals("type") || propertyName.equals("_class"))
			return;
		if (! session.getBeanContext().isIgnoreUnknownBeanProperties())
			throw new ParseException(session, "Unknown property ''{0}'' encountered while trying to parse into class ''{1}''", propertyName, beanMap.getClassMeta());
		if (listeners.size() > 0)
			for (ParserListener listener : listeners)
				listener.onUnknownProperty(propertyName, beanMap.getClassMeta().getInnerClass(), beanMap.getBean(), line, col);
	}


	/**
	 * Returns the media types handled based on the value of the {@link Consumes} annotation on the parser class.
	 * <p>
	 * This method can be overridden by subclasses to determine the media types programatically.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public String[] getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * Returns the results from {@link #getMediaTypes()} parsed as {@link MediaRange MediaRanges}.
	 *
	 * @return The list of media types parsed as ranges.  Never <jk>null</jk>.
	 */
	public MediaRange[] getMediaRanges() {
		return mediaRanges;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public Parser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public Parser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public Parser addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public Parser addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> Parser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public Parser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public Parser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public Parser clone() throws CloneNotSupportedException {
		Parser c = (Parser)super.clone();
		return c;
	}
}
