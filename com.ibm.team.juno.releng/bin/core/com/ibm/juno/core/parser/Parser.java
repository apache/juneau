/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import static com.ibm.juno.core.utils.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.utils.*;

/**
 * Parent class for all Juno parsers.
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
 * 	In addition, any class types with {@link PojoFilter PojoFilters} associated with them on the registered
 * 		{@link #getBeanContext() beanContext} can also be passed in.
 * <p>
 * 	For example, if the {@link CalendarFilter} filter is used to generalize {@code Calendar} objects to {@code String} objects.  When registered
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
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <R> The input type (e.g. Reader or InputStream)
 */
public abstract class Parser<R> extends CoreApi {

	/** General serializer properties currently set on this serializer. */
	protected transient ParserProperties pp = new ParserProperties();
	private transient List<ParserListener> listeners = new LinkedList<ParserListener>();
	private String[] mediaTypes;

	// Hidden constructor to force subclass from InputStreamParser or ReaderParser.
	Parser() {}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Workhorse method.  Subclasses are expected to implement this method.
	 *
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 * @param ctx The runtime context object returned by {@link #createContext(ObjectMap, Method, Object)}.
	 * 	If <jk>null</jk>, one will be created using {@link #createContext()}.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	protected abstract <T> T doParse(R in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException;

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
	 *
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 * @param ctx The runtime context object returned by {@link #createContext(ObjectMap, Method, Object)}.
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <T> T parse(R in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		try {
			if (in == null)
				throw new IOException("Null input stream or reader passed to parser.");
			return doParse(in, estimatedSize, type, ctx);
		} catch (RuntimeException e) {
			throw new ParseException(e);
		} finally {
			ctx.close();
		}
	}

	/**
	 * Parses the content of the reader and creates an object of the specified type.
	 * <p>
	 * Equivalent to calling <code>parser.parse(in, type, <jk>null</jk>);</code>
	 *
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param type The class type of the object to create.
	 * 	If <jk>null</jk> or <code>Object.<jk>class</jk></code>, object type is based on what's being parsed.
	 * 	For example, when parsing JSON text, it may return a <code>String</code>, <code>Number</code>, <code>ObjectMap</code>, etc...
	 * @param <T> The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <T> T parse(R in, int estimatedSize, ClassMeta<T> type) throws ParseException, IOException {
		ParserContext ctx = createContext();
		return parse(in, estimatedSize, type, ctx);
	}

	/**
	 * Parses input into the specified object type.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	MyBean b = p.parse(json, MyBean.<jk>class</jk>);
	 * </p>
	 * <p>
	 * This method equivalent to the following code:
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;MyBean&gt; cm = p.getBeanContext().getClassMeta(MyBean.<jk>class</jk>);
	 * 	MyBean b = p.parse(json, cm, <jk>null</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param type The class type of the object to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <T> T parse(R in, int estimatedSize, Class<T> type) throws ParseException, IOException {
		ClassMeta<T> cm = getBeanContext().getClassMeta(type);
		return parse(in, estimatedSize, cm);
	}

	/**
	 * Parses input into a map with specified key and value types.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	Map&lt;String,MyBean&gt; m = p.parseMap(json, LinkedHashMap.<jk>class</jk>, String.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * 		<p>
	 * 			A simpler approach is often to just extend the map class you want and just use the normal {@link #parse(Object, int, Class)} method:
	 * </p>
	 * <p class='bcode'>
	 * 	<jk>public static class</jk> MyMap <jk>extends</jk> LinkedHashMap&lt;String,MyBean&gt; {}
	 *
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	Map&lt;String,MyBean&gt; m = p.parse(json, MyMap.<jk>class</jk>);
	 * </p>
	 * <p>
	 * This method equivalent to the following code:
	 * 		</p>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;Map&lt;String,MyBean&gt;&gt; cm = p.getBeanContext().getMapClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	Map&ltString,MyBean&gt; m = p.parse(json, cm, <jk>null</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param mapClass The map class type.
	 * @param keyClass The key class type.
	 * @param valueClass The value class type.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <K,V,T extends Map<K,V>> T parseMap(R in, int estimatedSize, Class<T> mapClass, Class<K> keyClass, Class<V> valueClass) throws ParseException, IOException {
		ClassMeta<T> cm = getBeanContext().getMapClassMeta(mapClass, keyClass, valueClass);
		return parse(in, estimatedSize, cm);
	}

	/**
	 * Parses input into a collection with a specified element type.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	List&lt;MyBean&gt; l = p.parseCollection(json, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * 		<p>
	 * A simpler approach is often to just extend the collection class you want and just use the normal {@link #parse(Object, int, Class)} method:
	 * 		</p>
	 * <p class='bcode'>
	 * 	<jk>public static class</jk> MyBeanCollection <jk>extends</jk> LinkedList&lt;MyBean&gt; {}
	 *
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	List&lt;MyBean&gt; l = p.parse(json, MyBeanCollection.<jk>class</jk>);
	 * </p>
	 * <p>
	 * This method equivalent to the following code:
	 * 		</p>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 	ClassMeta&lt;List&lt;MyBean&gt;&gt; cm = p.getBeanContext().getCollectionClassMeta(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	List&lt;MyBean&gt; l = p.parse(json, cm, <jk>null</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the object to create.
	 * @param in The input stream or reader containing the input.
	 * @param estimatedSize The estimated size of the input, or <code>-1</code> if unknown.
	 * @param collectionClass The collection class type.
	 * @param entryClass The class type of entries in the collection.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public final <E,T extends Collection<E>> T parseCollection(R in, int estimatedSize, Class<T> collectionClass, Class<E> entryClass) throws ParseException, IOException {
		ClassMeta<T> cm = getBeanContext().getCollectionClassMeta(collectionClass, entryClass);
		return parse(in, estimatedSize, cm);
	}

	/**
	 * Create the context object that will be passed in to the parse method.
	 * <p>
	 * 	It's up to implementers to decide what the context object looks like, although typically
	 * 	it's going to be a subclass of {@link ParserContext}.
	 *
	 * @param properties Optional additional properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @return The new context.
	 */
	public ParserContext createContext(ObjectMap properties, Method javaMethod, Object outer) {
		return new ParserContext(getBeanContext(), pp, properties, javaMethod, outer);
	}

	/**
	 * Create a basic context object without overriding properties or specifying <code>javaMethod</code>.
	 * <p>
	 * Equivalent to calling <code>createContext(<jk>null</jk>, <jk>null</jk>)</code>.
	 *
	 * @return The new context.
	 */
	protected final ParserContext createContext() {
		return createContext(null, null, null);
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
	public Parser<R> addListener(ParserListener listener) throws LockedException {
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
	 * @param outer The outer object if we're converting to an inner object that needs to be created within the context of an outer object.
	 * @param s The string to convert.
	 * @param type The class type to convert the string to.
	 * @return The string converted as an object of the specified type.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @param <T> The class type to convert the string to.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T> T convertAttrToType(Object outer, String s, ClassMeta<T> type) throws ParseException {
		if (s == null)
			return null;

		if (type == null)
			type = (ClassMeta<T>)object();
		PojoFilter filter = type.getPojoFilter();
		ClassMeta<?> gType = type.getFilteredClassMeta();

		Object o = s;
		if (gType.isChar())
			o = s.charAt(0);
		else if (gType.isNumber())
			o = parseNumber(s, (Class<? extends Number>)gType.getInnerClass());
		else if (gType.isBoolean())
			o = Boolean.parseBoolean(s);
		else if (! (gType.isCharSequence() || gType.isObject())) {
			if (gType.canCreateNewInstanceFromString(outer)) {
				try {
					o = gType.newInstanceFromString(outer, s);
				} catch (Exception e) {
					throw new ParseException("Unable to construct new object of type ''{0}'' from input string ''{1}''", type, s).initCause(e);
				}
			} else {
				throw new ParseException("Invalid conversion from string to class ''{0}''", type);
			}
		}

		if (filter != null)
			o = filter.unfilter(o, type);


		return (T)o;
	}

	/**
	 * Convenience method for calling the {@link ParentProperty @ParentProperty} method on
	 * the specified object if it exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param parent The parent to set.
	 * @throws ParseException
	 */
	protected void setParent(ClassMeta<?> cm, Object o, Object parent) throws ParseException {
		Method m = cm.getParentProperty();
		if (m != null) {
			try {
				m.invoke(o, parent);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}
	}

	/**
	 * Convenience method for calling the {@link NameProperty @NameProperty} method on
	 * the specified object if it exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param name The name to set.
	 * @throws ParseException
	 */
	protected void setName(ClassMeta<?> cm, Object o, Object name) throws ParseException {
		Method m = cm.getNameProperty();
		if (m != null) {
			try {
				m.invoke(o, name);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}
	}


	/**
	 * Method that gets called when an unknown bean property name is encountered.
	 *
	 * @param ctx The parser context.
	 * @param propertyName The unknown bean property name.
	 * @param beanMap The bean that doesn't have the expected property.
	 * @param line The line number where the property was found.  <code>-1</code> if line numbers are not available.
	 * @param col The column number where the property was found.  <code>-1</code> if column numbers are not available.
	 * @throws ParseException Automatically thrown if {@link BeanContextProperties#BEAN_ignoreUnknownBeanProperties} setting
	 * 	on this parser is <jk>false</jk>
	 * @param <T> The class type of the bean map that doesn't have the expected property.
	 */
	protected <T> void onUnknownProperty(ParserContext ctx, String propertyName, BeanMap<T> beanMap, int line, int col) throws ParseException {
		if (propertyName.equals("uri") || propertyName.equals("type") || propertyName.equals("_class"))
			return;
		if (! ctx.getBeanContext().isIgnoreUnknownBeanProperties())
			throw new ParseException(line, col, "Unknown property ''{0}'' encountered while trying to parse into class ''{1}''", propertyName, beanMap.getClassMeta());
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
		if (mediaTypes == null) {
			Consumes c = ReflectionUtils.getAnnotation(Consumes.class, getClass());
			if (c == null)
				throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Consumes annotation", getClass().getName()));
			mediaTypes = c.value();
		}
		return mediaTypes;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------


	@Override /* CoreApi */
	public Parser<R> setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public Parser<R> addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public Parser<R> addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> Parser<R> addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public Parser<R> setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public Parser<R> lock() {
		super.lock();
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override /* Lockable */
	public Parser<R> clone() throws CloneNotSupportedException {
		return (Parser<R>)super.clone();
	}
}
