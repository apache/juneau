/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.utils.*;

/**
 * Subclass of {@link Parser} for characters-based parsers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This class is typically the parent class of all character-based parsers.
 * 	It has 1 abstract method to implement...
 * <ul>
 * 	<li><code>parse(Reader, ClassMeta, ParserContext)</code>
 * </ul>
 *
 *
 * <h6 class='topic'>@Consumes annotation</h6>
 * <p>
 * 	The media types that this parser can handle is specified through the {@link Consumes @Consumes} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()} method.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class ReaderParser extends Parser<Reader> {

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	protected abstract <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException;

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Same as <code>parse(Reader, Class)</code> except parses from a <code>CharSequence</code>.
	 *
	 * @param in The string containing the input.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public <T> T parse(CharSequence in, Class<T> type) throws ParseException {
		return parse(in, getBeanContext().getClassMeta(type));
	}

	/**
	 * Same as <code>parse(Reader, ClassMeta)</code> except parses from a <code>CharSequence</code>.
	 *
	 * @param in The string containing the input.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public <T> T parse(CharSequence in, ClassMeta<T> type) throws ParseException {
		try {
			if (in == null)
				return null;
			return parse(wrapReader(in), in.length(), type, createContext());
		} catch (IOException e) {
			throw new ParseException(e); // Won't happen since it's a StringReader.
		}
	}

	/**
	 * Same as <code>parseMap(Reader, Class, Class, Class)</code> except parses from a <code>CharSequence</code>.
	 *
	 * @param <T> The map class type.
	 * @param <K> The class type of the map keys.
	 * @param <V> The class type of the map values.
	 * @param in The string containing the input.
	 * @param mapClass The map class type.
	 * @param keyClass The class type of the map keys.
	 * @param valueClass The class type of the map values.
	 * @return A new map instance.
	 * @throws ParseException
	 */
	public <K,V,T extends Map<K,V>> T parseMap(CharSequence in, Class<T> mapClass, Class<K> keyClass, Class<V> valueClass) throws ParseException {
		ClassMeta<T> cm = getBeanContext().getMapClassMeta(mapClass, keyClass, valueClass);
		return parse(in, cm);
	}

	/**
	 * Same as <code>parseCollection(Reader, Class, Class)</code> except parses from a <code>CharSequence</code>.
	 *
	 * @param <T> The collection class type.
	 * @param <E> The class type of the collection entries.
	 * @param in The string containing the input.
	 * @param collectionClass The map class type.
	 * @param entryClass collectionClass
	 * @return A new collection instance.
	 * @throws ParseException
	 */
	public <E,T extends Collection<E>> T parseCollection(CharSequence in, Class<T> collectionClass, Class<E> entryClass) throws ParseException {
		ClassMeta<T> cm = getBeanContext().getCollectionClassMeta(collectionClass, entryClass);
		return parse(in, cm);
	}

	/**
	 * Wraps the specified character sequence inside a reader.
	 * Subclasses can override this method to implement their own readers.
	 *
	 * @param in The string being wrapped.
	 * @return The string wrapped in a reader, or <jk>null</jk> if the <code>CharSequence</code> is null.
	 */
	protected Reader wrapReader(CharSequence in) {
		return new CharSequenceReader(in);
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
	 * <ul>
	 * 	<li>The various character-based constructors in {@link ObjectMap} (e.g. {@link ObjectMap#ObjectMap(CharSequence, ReaderParser)}).
	 * </ul>
	 *
	 * @param <K> The key class type.
	 * @param <V> The value class type.
	 * @param in The reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.<br>
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.<br>
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <K,V> Map<K,V> parseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType) throws ParseException, IOException {
		ParserContext ctx = createContext();
		try {
			if (in == null)
				throw new IOException("Null input stream or reader passed to parser.");
			return doParseIntoMap(in, estimatedSize, m, keyType, valueType, ctx);
		} finally {
			ctx.close();
		}
	}

	/**
	 * Implementation method.
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param in The input.  Must represent an array.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.<br>
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.<br>
	 * @param ctx The runtime context object returned by {@link #createContext(ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createContext()}.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException Occurs if syntax error detected in input.
	 * @throws IOException Occurs if thrown from <code>Reader</code>
	 */
	protected <K,V> Map<K,V> doParseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType, ParserContext ctx) throws ParseException, IOException {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Same as {@link #parseIntoMap(Reader, int, Map, Type, Type)} except reads from a <code>CharSequence</code>.
	 *
	 * @param in The input.  Must represent an array.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.<br>
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.<br>
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException Occurs if syntax error detected in input.
	 */
	public final <K,V> Map<K,V> parseIntoMap(CharSequence in, Map<K,V> m, Type keyType, Type valueType) throws ParseException {
		try {
			if (in == null)
				return null;
			return parseIntoMap(wrapReader(in), in.length(), m, keyType, valueType);
		} catch (IOException e) {
			throw new ParseException(e);  // Won't happen.
		}
	}

	/**
	 * Parses the contents of the specified reader and loads the results into the specified collection.
	 * <p>
	 * 	Used in the following locations:
	 * <ul>
	 * 	<li>The various character-based constructors in {@link ObjectList} (e.g. {@link ObjectList#ObjectList(CharSequence, ReaderParser)}.
	 * </ul>
	 *
	 * @param <E> The element class type.
	 * @param in The reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <E> Collection<E> parseIntoCollection(Reader in, int estimatedSize, Collection<E> c, Type elementType) throws ParseException, IOException {
		ParserContext ctx = createContext();
		try {
			if (in == null)
				throw new IOException("Null reader passed to parser.");
			return doParseIntoCollection(in, estimatedSize, c, elementType, ctx);
		} finally {
			ctx.close();
		}
	}

	/**
	 * Implementation method.
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param in The input.  Must represent an array.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @param ctx The runtime context object returned by {@link #createContext(ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createContext()}.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException Occurs if syntax error detected in input.
	 * @throws IOException Occurs if thrown from <code>Reader</code>
	 */
	protected <E> Collection<E> doParseIntoCollection(Reader in, int estimatedSize, Collection<E> c, Type elementType, ParserContext ctx) throws ParseException, IOException {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Same as {@link #parseIntoCollection(Reader, int, Collection, Type)} except reads from a <code>CharSequence</code>.
	 *
	 * @param in The input.  Must represent an array.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException Occurs if syntax error detected in input.
	 */
	public final <E> Collection<E> parseIntoCollection(CharSequence in, Collection<E> c, Type elementType) throws ParseException {
		try {
			return parseIntoCollection(wrapReader(in), in.length(), c, elementType);
		} catch (IOException e) {
			throw new ParseException(e);  // Won't happen.
		}
	}

	/**
	 * Parses the specified array input with each entry in the object defined by the {@code argTypes}
	 * argument.
	 * <p>
	 * 	Used for converting arrays (e.g. <js>"[arg1,arg2,...]"</js>) into an {@code Object[]} that can be passed
	 * 	to the {@code Method.invoke(target, args)} method.
	 * <p>
	 * 	Used in the following locations:
	 * <ul>
	 * 	<li>Used to parse argument strings in the {@link PojoIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 *
	 * @param in The input.  Must represent an array.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final Object[] parseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes) throws ParseException, IOException {
		if (in == null)
			throw new IOException("Null reader passed to parser.");
		if (argTypes == null || argTypes.length == 0)
			return new Object[0];
		ParserContext ctx = createContext();
		try {
			return doParseArgs(in, estimatedSize, argTypes, ctx);
		} finally {
			ctx.close();
		}
	}

	/**
	 * Implementation method.
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param in The input.  Must represent an array.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @param ctx The runtime context object returned by {@link #createContext(ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createContext()}.
	 * @return An array of parsed objects.
	 * @throws ParseException Occurs if syntax error detected in input.
	 * @throws IOException Occurs if thrown from <code>Reader</code>
	 */
	protected Object[] doParseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes, ParserContext ctx) throws ParseException, IOException {
		throw new UnsupportedOperationException("Parser '"+getClass().getName()+"' does not support this method.");
	}

	/**
	 * Same as {@link #parseArgs(Reader, int, ClassMeta[])} except reads from a <code>CharSequence</code>.
	 *
	 * @param in The input.  Must represent an array.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException Occurs if syntax error detected in input.
	 */
	public Object[] parseArgs(CharSequence in, ClassMeta<?>[] argTypes) throws ParseException {
		try {
			return parseArgs(wrapReader(in), in.length(), argTypes);
		} catch (IOException e) {
			throw new ParseException(e);  // Won't happen.
		}
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public boolean isReaderParser() {
		return true;
	}

	@Override /* Parser */
	public ReaderParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public ReaderParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public ReaderParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public ReaderParser addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> ReaderParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public ReaderParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public ReaderParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public ReaderParser clone() throws CloneNotSupportedException {
		return (ReaderParser)super.clone();
	}
}
