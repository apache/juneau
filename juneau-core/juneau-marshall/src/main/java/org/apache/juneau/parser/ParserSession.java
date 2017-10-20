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
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Session object that lives for the duration of a single use of {@link Parser}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused against multiple inputs.
 */
public abstract class ParserSession extends BeanSession {

	private final boolean trimStrings, strict;
	private final String inputStreamCharset, fileCharset;
	private final Method javaMethod;
	private final Object outer;

	// Writable properties.
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;
	private final ParserListener listener;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected ParserSession(ParserContext ctx, ParserSessionArgs args) {
		super(ctx != null ? ctx : ParserContext.DEFAULT, args);
		if (ctx == null)
			ctx = ParserContext.DEFAULT;
		Class<?> listenerClass;
		ObjectMap p = getProperties();
		trimStrings = p.getBoolean(PARSER_trimStrings, ctx.trimStrings);
		strict = p.getBoolean(PARSER_strict, ctx.strict);
		inputStreamCharset = p.getString(PARSER_inputStreamCharset, ctx.inputStreamCharset);
		fileCharset = p.getString(PARSER_fileCharset, ctx.fileCharset);
		listenerClass = p.getWithDefault(PARSER_listener, ctx.listener, Class.class);
		this.javaMethod = args.javaMethod;
		this.outer = args.outer;
		this.listener = newInstance(ParserListener.class, listenerClass);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("ParserSession", new ObjectMap()
				.append("fileCharset", fileCharset)
				.append("inputStreamCharset", inputStreamCharset)
				.append("javaMethod", javaMethod)
				.append("listener", listener)
				.append("outer", outer)
				.append("strict", strict)
				.append("trimStrings", trimStrings)
			);
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Workhorse method.  Subclasses are expected to implement this method.
	 *
	 * @param pipe Where to get the input from.
	 * @param type
	 * 	The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>,
	 * 	<code>ObjectMap</code>, etc...
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected abstract <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception;

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
	 * Wraps the specified input object into a {@link ParserPipe} object so that it can be easily converted into
	 * a stream or reader.
	 *
	 * @param input
	 * 	The input.
	 * 	<br>For character-based parsers, this can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link Parser#PARSER_inputStreamCharset}).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link Parser#PARSER_inputStreamCharset}).
	 * 		<li>{@link File} containing system encoded text (or whatever the encoding specified by
	 * 			{@link Parser#PARSER_fileCharset}).
	 * 	</ul>
	 * 	<br>For byte-based parsers, this can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	public final ParserPipe createPipe(Object input) {
		return new ParserPipe(input, isDebug(), strict, fileCharset, inputStreamCharset);
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <code>{line:123,column:456,currentProperty:"foobar"}</code>
	 */
	public final ObjectMap getLastLocation() {
		ObjectMap m = new ObjectMap();
		if (currentClass != null)
			m.put("currentClass", currentClass.toString(true));
		if (currentProperty != null)
			m.put("currentProperty", currentProperty);
		return m;
	}

	/**
	 * Returns the Java method that invoked this parser.
	 *
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 *
	 * @return The Java method that invoked this parser.
	*/
	protected final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the outer object used for instantiating top-level non-static member classes.
	 *
	 * <p>
	 * When using the REST API, this is the servlet object.
	 *
	 * @return The outer object.
	*/
	protected final Object getOuter() {
		return outer;
	}

	/**
	 * Sets the current bean property being parsed for proper error messages.
	 *
	 * @param currentProperty The current property being parsed.
	 */
	protected final void setCurrentProperty(BeanPropertyMeta currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being parsed for proper error messages.
	 *
	 * @param currentClass The current class being parsed.
	 */
	protected final void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Returns the {@link Parser#PARSER_trimStrings} setting value for this session.
	 *
	 * @return The {@link Parser#PARSER_trimStrings} setting value for this session.
	 */
	protected final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the {@link Parser#PARSER_strict} setting value for this session.
	 *
	 * @return The {@link Parser#PARSER_strict} setting value for this session.
	 */
	protected final boolean isStrict() {
		return strict;
	}

	/**
	 * Trims the specified object if it's a <code>String</code> and {@link #isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The object to trim.
	 * @return The trimmed string if it's a string.
	 */
	@SuppressWarnings("unchecked")
	protected final <K> K trim(K o) {
		if (trimStrings && o instanceof String)
			return (K)o.toString().trim();
		return o;

	}

	/**
	 * Trims the specified string if {@link ParserSession#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param s The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	protected final String trim(String s) {
		if (trimStrings && s != null)
			return s.trim();
		return s;
	}

	/**
	 * Converts the specified <code>ObjectMap</code> into a bean identified by the <js>"_type"</js> property in the map.
	 *
	 * @param m The map to convert to a bean.
	 * @param pMeta The current bean property being parsed.
	 * @param eType The current expected type being parsed.
	 * @return
	 * 	The converted bean, or the same map if the <js>"_type"</js> entry wasn't found or didn't resolve to a bean.
	 */
	protected final Object cast(ObjectMap m, BeanPropertyMeta pMeta, ClassMeta<?> eType) {

		String btpn = getBeanTypePropertyName(eType);

		Object o = m.get(btpn);
		if (o == null)
			return m;
		String typeName = o.toString();

		ClassMeta<?> cm = getClassMeta(typeName, pMeta, eType);

		if (cm != null) {
			BeanMap<?> bm = m.getBeanSession().newBeanMap(cm.getInnerClass());

			// Iterate through all the entries in the map and set the individual field values.
			for (Map.Entry<String,Object> e : m.entrySet()) {
				String k = e.getKey();
				Object v = e.getValue();
				if (! k.equals(btpn)) {
					// Attempt to recursively cast child maps.
					if (v instanceof ObjectMap)
						v = cast((ObjectMap)v, pMeta, eType);
					bm.put(k, v);
				}
			}
			return bm.getBean();
		}

		return m;
	}

	/**
	 * Give the specified dictionary name, resolve it to a class.
	 *
	 * @param typeName The dictionary name to resolve.
	 * @param pMeta The bean property we're currently parsing.
	 * @param eType The expected type we're currently parsing.
	 * @return The resolved class, or <jk>null</jk> if the type name could not be resolved.
	 */
	protected final ClassMeta<?> getClassMeta(String typeName, BeanPropertyMeta pMeta, ClassMeta<?> eType) {
		BeanRegistry br = null;

		// Resolve via @BeanProperty(beanDictionary={})
		if (pMeta != null) {
			br = pMeta.getBeanRegistry();
			if (br != null && br.hasName(typeName))
				return br.getClassMeta(typeName);
		}

		// Resolve via @Bean(beanDictionary={}) on the expected type where the
		// expected type is an interface with subclasses.
		if (eType != null) {
			br = eType.getBeanRegistry();
			if (br != null && br.hasName(typeName))
				return br.getClassMeta(typeName);
		}

		// Last resort, resolve using the session registry.
		return getBeanRegistry().getClassMeta(typeName);
	}

	/**
	 * Method that gets called when an unknown bean property name is encountered.
	 *
	 * @param pipe The parser input.
	 * @param propertyName The unknown bean property name.
	 * @param beanMap The bean that doesn't have the expected property.
	 * @param line The line number where the property was found.  <code>-1</code> if line numbers are not available.
	 * @param col The column number where the property was found.  <code>-1</code> if column numbers are not available.
	 * @throws ParseException
	 * 	Automatically thrown if {@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting on this parser is
	 * 	<jk>false</jk>
	 * @param <T> The class type of the bean map that doesn't have the expected property.
	 */
	protected final <T> void onUnknownProperty(ParserPipe pipe, String propertyName, BeanMap<T> beanMap, int line, int col) throws ParseException {
		if (propertyName.equals(getBeanTypePropertyName(beanMap.getClassMeta())))
			return;
		if (! isIgnoreUnknownBeanProperties())
			throw new ParseException(getLastLocation(),
				"Unknown property ''{0}'' encountered while trying to parse into class ''{1}''", propertyName,
				beanMap.getClassMeta());
		if (listener != null)
			listener.onUnknownBeanProperty(this, pipe, propertyName, beanMap.getClassMeta().getInnerClass(), beanMap.getBean(),
				line, col);
	}

	/**
	 * Parses input into the specified object type.
	 *
	 * <p>
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
	 *
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Use the {@link #parse(Object, Class)} method instead if you don't need a parameterized map/collection.
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
	 * 			{@link Parser#PARSER_inputStreamCharset} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link Parser#PARSER_inputStreamCharset} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link Parser#PARSER_fileCharset} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T parse(Object input, Type type, Type...args) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return (T)parseInner(pipe, getClassMeta(type, args));
		} finally {
			pipe.close();
		}
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
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
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, Class<T> type) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return parseInner(pipe, getClassMeta(type));
		} finally {
			pipe.close();
		}
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
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return parseInner(pipe, type);
		} finally {
			pipe.close();
		}
	}

	/**
	 * Entry point for all parsing calls.
	 *
	 * <p>
	 * Calls the {@link #doParse(ParserPipe, ClassMeta)} implementation class and catches/re-wraps any exceptions
	 * thrown.
	 *
	 * @param pipe The parser input.
	 * @param type The class type of the object to create.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	private <T> T parseInner(ParserPipe pipe, ClassMeta<T> type) throws ParseException {
		try {
			if (type.isVoid())
				return null;
			return doParse(pipe, type);
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(getLastLocation(), "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(getLastLocation(), "I/O exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} catch (Exception e) {
			throw new ParseException(getLastLocation(), "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		}
	}

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
	 * 		The various character-based constructors in {@link ObjectMap} (e.g.
	 * 		{@link ObjectMap#ObjectMap(CharSequence,Parser)}).
	 * </ul>
	 *
	 * @param <K> The key class type.
	 * @param <V> The value class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <K,V> Map<K,V> parseIntoMap(Object input, Map<K,V> m, Type keyType, Type valueType) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return doParseIntoMap(pipe, m, keyType, valueType);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(getLastLocation(), e);
		} finally {
			pipe.close();
		}
	}

	/**
	 * Implementation method.
	 *
	 * <p>
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param pipe The parser input.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Parses the contents of the specified reader and loads the results into the specified collection.
	 *
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The various character-based constructors in {@link ObjectList} (e.g.
	 * 		{@link ObjectList#ObjectList(CharSequence,Parser)}.
	 * </ul>
	 *
	 * @param <E> The element class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <E> Collection<E> parseIntoCollection(Object input, Collection<E> c, Type elementType) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return doParseIntoCollection(pipe, c, elementType);
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(getLastLocation(), "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(getLastLocation(), "I/O exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} catch (Exception e) {
			throw new ParseException(getLastLocation(), "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} finally {
			pipe.close();
		}
	}

	/**
	 * Implementation method.
	 *
	 * <p>
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param pipe The parser input.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
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
	 * 		Used to parse argument strings in the {@link PojoIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 *
	 * @param input The input.  Subclasses can support different input types.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final Object[] parseArgs(Object input, Type[] argTypes) throws ParseException {
		ParserPipe pipe = createPipe(input);
		try {
			return doParse(pipe, getArgsClassMeta(argTypes));
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(getLastLocation(), "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(getLastLocation(), "I/O exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} catch (Exception e) {
			throw new ParseException(getLastLocation(), "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage()).initCause(e);
		} finally {
			pipe.close();
		}
	}

	/**
	 * Converts the specified string to the specified type.
	 *
	 * @param outer
	 * 	The outer object if we're converting to an inner object that needs to be created within the context
	 * 	of an outer object.
	 * @param s The string to convert.
	 * @param type The class type to convert the string to.
	 * @return The string converted as an object of the specified type.
	 * @throws Exception If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @param <T> The class type to convert the string to.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "hiding" })
	protected final <T> T convertAttrToType(Object outer, String s, ClassMeta<T> type) throws Exception {
		if (s == null)
			return null;

		if (type == null)
			type = (ClassMeta<T>)object();
		PojoSwap swap = type.getPojoSwap(this);
		ClassMeta<?> sType = swap == null ? type : swap.getSwapClassMeta(this);

		Object o = s;
		if (sType.isChar())
			o = s.charAt(0);
		else if (sType.isNumber())
			if (type.canCreateNewInstanceFromNumber(outer))
				o = type.newInstanceFromNumber(this, outer, parseNumber(s, type.getNewInstanceFromNumberClass()));
			else
				o = parseNumber(s, (Class<? extends Number>)sType.getInnerClass());
		else if (sType.isBoolean())
			o = Boolean.parseBoolean(s);
		else if (! (sType.isCharSequence() || sType.isObject())) {
			if (sType.canCreateNewInstanceFromString(outer))
				o = sType.newInstanceFromString(outer, s);
			else
				throw new ParseException(getLastLocation(), "Invalid conversion from string to class ''{0}''", type);
		}

		if (swap != null)
			o = swap.unswap(this, o, type);

		return (T)o;
	}

	/**
	 * Convenience method for calling the {@link ParentProperty @ParentProperty} method on the specified object if it
	 * exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param parent The parent to set.
	 * @throws Exception
	 */
	protected final static void setParent(ClassMeta<?> cm, Object o, Object parent) throws Exception {
		Setter m = cm.getParentProperty();
		if (m != null)
			m.set(o, parent);
	}

	/**
	 * Convenience method for calling the {@link NameProperty @NameProperty} method on the specified object if it exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param name The name to set.
	 * @throws Exception
	 */
	protected final static void setName(ClassMeta<?> cm, Object o, Object name) throws Exception {
		if (cm != null) {
			Setter m = cm.getNameProperty();
			if (m != null)
				m.set(o, name);
		}
	}
}
