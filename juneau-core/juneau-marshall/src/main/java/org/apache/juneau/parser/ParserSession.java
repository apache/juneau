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
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link Parser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class ParserSession extends BeanSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(Parser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanSession.Builder {

		Parser ctx;
		Method javaMethod;
		Object outer;
		HttpPartSchema schema;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Parser ctx) {
			super(ctx.getBeanContext());
			this.ctx = ctx;
			mediaTypeDefault(ctx.getPrimaryMediaType());
		}

		@Override
		public ParserSession build() {
			return new ParserSession(this);
		}

		/**
		 * The java method that called this serializer, usually the method in a REST servlet.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder javaMethod(Method value) {
			this.javaMethod = value;
			return this;
		}

		/**
		 * The outer object for instantiating top-level non-static inner classes.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder outer(Object value) {
			this.outer = value;
			return this;
		}

		/**
		 * HTTP-part schema.
		 *
		 * <p>
		 * Used for schema-based serializers and parsers to define additional formatting.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder schema(HttpPartSchema value) {
			if (value != null)
				this.schema = value;
			return this;
		}

		/**
		 * Same as {@link #schema(HttpPartSchema)} but doesn't overwrite the value if it is already set.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
		 * @return This object.
		 */
		@FluentSetter
		public Builder schemaDefault(HttpPartSchema value) {
			if (value != null && schema == null)
				this.schema = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Parser ctx;
	private final Method javaMethod;
	private final Object outer;
	private final Stack<StringBuilder> sbStack;
	private final HttpPartSchema schema;

	// Writable properties.
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;
	private final ParserListener listener;

	private Position mark = new Position(-1);

	private ParserPipe pipe;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		javaMethod = builder.javaMethod;
		outer = builder.outer;
		schema = builder.schema;
		listener = BeanCreator.of(ParserListener.class).type(ctx.getListener()).orElse(null);
		sbStack = new Stack<>();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Workhorse method.
	 *
	 * <p>
	 * Subclasses are expected to implement this method or {@link Parser#doParse(ParserSession,ParserPipe,ClassMeta)}.
	 *
	 * <p>
	 * The default implementation of this method simply calls {@link Parser#doParse(ParserSession,ParserPipe,ClassMeta)}.
	 *
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
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		return ctx.doParse(this, pipe, type);
	}

	/**
	 * Returns <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 *
	 * @return <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 */
	public boolean isReaderParser() {
		return false;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)}).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)}).
	 * 		<li>{@link File} containing system encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder#fileCharset(Charset)}).
	 * 	</ul>
	 * 	<br>For byte-based parsers, this can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link InputStreamParser.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	protected ParserPipe createPipe(Object input) {
		return null;
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <c>{line:123,column:456,currentProperty:"foobar"}</c>
	 */
	public final JsonMap getLastLocation() {
		JsonMap m = new JsonMap();
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
	 * Trims the specified object if it's a <c>String</c> and {@link #isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param <K> The object type.
	 * @param o The object to trim.
	 * @return The trimmed string if it's a string.
	 */
	@SuppressWarnings("unchecked")
	protected final <K> K trim(K o) {
		if (isTrimStrings() && o instanceof String)
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
		if (isTrimStrings() && s != null)
			return s.trim();
		return s;
	}

	/**
	 * Converts the specified <c>JsonMap</c> into a bean identified by the <js>"_type"</js> property in the map.
	 *
	 * @param m The map to convert to a bean.
	 * @param pMeta The current bean property being parsed.
	 * @param eType The current expected type being parsed.
	 * @return
	 * 	The converted bean, or the same map if the <js>"_type"</js> entry wasn't found or didn't resolve to a bean.
	 */
	protected final Object cast(JsonMap m, BeanPropertyMeta pMeta, ClassMeta<?> eType) {

		String btpn = getBeanTypePropertyName(eType);

		Object o = m.get(btpn);
		if (o == null)
			return m;
		String typeName = o.toString();

		ClassMeta<?> cm = getClassMeta(typeName, pMeta, eType);

		if (cm != null) {
			BeanMap<?> bm = m.getBeanSession().newBeanMap(cm.getInnerClass());

			// Iterate through all the entries in the map and set the individual field values.
			m.forEach((k,v) -> {
				if (! k.equals(btpn)) {
					// Attempt to recursively cast child maps.
					if (v instanceof JsonMap)
						v = cast((JsonMap)v, pMeta, eType);
					bm.put(k, v);
				}
			});
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

		// Resolve via @Beanp(dictionary={})
		if (pMeta != null) {
			br = pMeta.getBeanRegistry();
			if (br != null && br.hasName(typeName))
				return br.getClassMeta(typeName);
		}

		// Resolve via @Bean(dictionary={}) on the expected type where the
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
	 * Specialized warning when an exception is thrown while executing a bean setter.
	 *
	 * @param p The bean map entry representing the bean property.
	 * @param t The throwable that the bean setter threw.
	 */
	protected final void onBeanSetterException(BeanPropertyMeta p, Throwable t) {
		if (listener != null)
			listener.onBeanSetterException(this, t, p);
		String prefix = "";
		addWarning("{0}Could not call setValue() on property ''{1}'' of class ''{2}'', exception = {3}", prefix,
			p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage());
	}

	/**
	 * Method that gets called when an unknown bean property name is encountered.
	 *
	 * @param propertyName The unknown bean property name.
	 * @param beanMap The bean that doesn't have the expected property.
	 * @param value The parsed value.
	 * @throws ParseException
	 * 	Automatically thrown if {@link org.apache.juneau.BeanContext.Builder#ignoreUnknownBeanProperties()} setting on this parser is
	 * 	<jk>false</jk>
	 * @param <T> The class type of the bean map that doesn't have the expected property.
	 */
	protected final <T> void onUnknownProperty(String propertyName, BeanMap<T> beanMap, Object value) throws ParseException {
		if (propertyName.equals(getBeanTypePropertyName(beanMap.getClassMeta())))
			return;
		if (! isIgnoreUnknownBeanProperties())
			if (value != null || ! isIgnoreUnknownNullBeanProperties())
				throw new ParseException(this,
					"Unknown property ''{0}'' encountered while trying to parse into class ''{1}''", propertyName,
					beanMap.getClassMeta());
		if (listener != null)
			listener.onUnknownBeanProperty(this, propertyName, beanMap.getClassMeta().getInnerClass(), beanMap.getBean());
	}

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
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 * @throws IOException Thrown by the underlying stream.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T parse(Object input, Type type, Type...args) throws ParseException, IOException {
		try (ParserPipe pipe = createPipe(input)) {
			return (T)parseInner(pipe, getClassMeta(type, args));
		}
	}

	/**
	 * Same as {@link #parse(Object,Type,Type...)} but parses from a string and doesn't throw an {@link IOException}.
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
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T parse(String input, Type type, Type...args) throws ParseException {
		try (ParserPipe pipe = createPipe(input)) {
			return (T)parseInner(pipe, getClassMeta(type, args));
		} catch (IOException e) {
			throw new ParseException(e); // Shouldn't happen.
		}
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
		try (ParserPipe pipe = createPipe(input)) {
			return parseInner(pipe, getClassMeta(type));
		}
	}

	/**
	 * Same as {@link #parse(Object, Class)} but parses from a string and doesn't throw an {@link IOException}.
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
	 */
	public final <T> T parse(String input, Class<T> type) throws ParseException {
		try (ParserPipe pipe = createPipe(input)) {
			return parseInner(pipe, getClassMeta(type));
		} catch (IOException e) {
			throw new ParseException(e); // Shouldn't happen.
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
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException, IOException {
		try (ParserPipe pipe = createPipe(input)) {
			return parseInner(pipe, type);
		}
	}

	/**
	 * Same as {@link #parse(Object, ClassMeta)} except parses from a string and doesn't throw an {@link IOException}.
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
	 */
	public final <T> T parse(String input, ClassMeta<T> type) throws ParseException {
		try (ParserPipe pipe = createPipe(input)) {
			return parseInner(pipe, type);
		} catch (IOException e) {
			throw new ParseException(e); // Shouldn't happen.
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
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by the underlying stream.
	 */
	private <T> T parseInner(ParserPipe pipe, ClassMeta<T> type) throws ParseException, IOException {
		if (type.isVoid())
			return null;
		try {
			return doParse(pipe, type);
		} catch (ParseException | IOException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(this, "Depth too deep.  Stack overflow occurred.");
		} catch (Exception e) {
			throw new ParseException(this, e, "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage());
		} finally {
			checkForWarnings();
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
		try (ParserPipe pipe = createPipe(input)) {
			return doParseIntoMap(pipe, m, keyType, valueType);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(this, e);
		} finally {
			checkForWarnings();
		}
	}

	/**
	 * Implementation method.
	 *
	 * <p>
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param pipe The parser input.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <K,V> Map<K,V> doParseIntoMap(ParserPipe pipe, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+className(getClass())+"' does not support this method.");
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
		try (ParserPipe pipe = createPipe(input)) {
			return doParseIntoCollection(pipe, c, elementType);
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(this, "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(this, e, "I/O exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage());
		} catch (Exception e) {
			throw new ParseException(this, e, "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage());
		} finally {
			checkForWarnings();
		}
	}

	/**
	 * Implementation method.
	 *
	 * <p>
	 * Default implementation throws an {@link UnsupportedOperationException}.
	 *
	 * @param <E> The element type.
	 * @param pipe The parser input.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected <E> Collection<E> doParseIntoCollection(ParserPipe pipe, Collection<E> c, Type elementType) throws Exception {
		throw new UnsupportedOperationException("Parser '"+className(getClass())+"' does not support this method.");
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
		try (ParserPipe pipe = createPipe(input)) {
			return doParse(pipe, getArgsClassMeta(argTypes));
		} catch (ParseException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new ParseException(this, "Depth too deep.  Stack overflow occurred.");
		} catch (IOException e) {
			throw new ParseException(this, e, "I/O exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage());
		} catch (Exception e) {
			throw new ParseException(this, e, "Exception occurred.  exception={0}, message={1}.",
				e.getClass().getSimpleName(), e.getLocalizedMessage());
		} finally {
			checkForWarnings();
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
	 * @param <T> The class type to convert the string to.
	 * @throws ParseException Malformed input encountered.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final <T> T convertAttrToType(Object outer, String s, ClassMeta<T> type) throws ParseException {
		if (s == null)
			return null;

		if (type == null)
			type = (ClassMeta<T>)object();
		ObjectSwap swap = type.getSwap(this);
		ClassMeta<?> sType = swap == null ? type : swap.getSwapClassMeta(this);

		Object o = s;
		if (sType.isChar())
			o = parseCharacter(s);
		else if (sType.isNumber())
			o = parseNumber(s, (Class<? extends Number>)sType.getInnerClass());
		else if (sType.isBoolean())
			o = Boolean.parseBoolean(s);
		else if (! (sType.isCharSequence() || sType.isObject())) {
			if (sType.canCreateNewInstanceFromString(outer))
				o = sType.newInstanceFromString(outer, s);
			else
				throw new ParseException(this, "Invalid conversion from string to class ''{0}''", type);
		}

		if (swap != null)
			o = unswap(swap, o, type);

		return (T)o;
	}

	/**
	 * Convenience method for calling the {@link ParentProperty @ParentProperty} method on the specified object if it
	 * exists.
	 *
	 * @param cm The class type of the object.
	 * @param o The object.
	 * @param parent The parent to set.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	protected static final void setParent(ClassMeta<?> cm, Object o, Object parent) throws ExecutableException {
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
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	protected static final void setName(ClassMeta<?> cm, Object o, Object name) throws ExecutableException {
		if (cm != null) {
			Setter m = cm.getNameProperty();
			if (m != null)
				m.set(o, name);
		}
	}

	/**
	 * Returns the listener associated with this session.
	 *
	 * @param <T> The listener type.
	 * @param c The listener class to cast to.
	 * @return The listener associated with this session, or <jk>null</jk> if there is no listener.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ParserListener> T getListener(Class<T> c) {
		return (T)listener;
	}

	/**
	 * The {@link #createPipe(Object)} method should call this method to set the pipe for debugging purposes.
	 *
	 * @param pipe The pipe created for this session.
	 * @return The same pipe.
	 */
	protected ParserPipe setPipe(ParserPipe pipe) {
		this.pipe = pipe;
		return pipe;
	}

	/**
	 * Returns the current position into the reader or input stream.
	 *
	 * @return
	 * 	The current position into the reader or input stream.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Position getPosition() {
		if (mark.line != -1 || mark.column != -1 || mark.position != -1)
			return mark;
		if (pipe == null)
			return Position.UNKNOWN;
		return pipe.getPosition();
	}

	/**
	 * Marks the current position.
	 */
	protected void mark() {
		if (pipe != null) {
			Position p = pipe.getPosition();
			mark.line = p.line;
			mark.column = p.column;
			mark.position = p.position;
		}
	}

	/**
	 * Unmarks the current position.
	 */
	protected void unmark() {
		mark.line = -1;
		mark.column = -1;
		mark.position = -1;
	}

	/**
	 * Returns the input as a string.
	 *
	 * <p>
	 * This always returns a value for input of type {@link CharSequence}.
	 * <br>For other input types, use {@link org.apache.juneau.Context.Builder#debug()} setting to enable caching to a string
	 * before parsing so that this method returns the input.
	 *
	 * @return The input as a string, or <jk>null</jk> if no pipe has been created or we're reading from an uncached reader or input stream source.
	 */
	public String getInputAsString() {
		return pipe == null ? null : pipe.getInputAsString();
	}

	/**
	 * Invokes the specified swap on the specified object.
	 *
	 * @param swap The swap to invoke.
	 * @param o The input object.
	 * @param eType The expected type.
	 * @return The swapped object.
	 * @throws ParseException If swap method threw an exception.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object unswap(ObjectSwap swap, Object o, ClassMeta<?> eType) throws ParseException {
		try {
			return swap.unswap(this, o, eType);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Creates a reusable {@link StringBuilder} object from an internal pool.
	 *
	 * <p>
	 * String builders are returned to the pool by calling {@link #returnStringBuilder(StringBuilder)}.
	 *
	 * @return A new or previously returned string builder.
	 */
	protected final StringBuilder getStringBuilder() {
		if (sbStack.isEmpty())
			return new StringBuilder();
		return sbStack.pop();
	}

	/**
	 * Returns a {@link StringBuilder} object back into the internal reuse pool.
	 *
	 * @param sb The string builder to return to the pool.  No-op if <jk>null</jk>.
	 */
	protected final void returnStringBuilder(StringBuilder sb) {
		if (sb == null)
			return;
		sb.setLength(0);
		sbStack.push(sb);
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
		return ctx.isAutoCloseStreams();
	}

	/**
	 * Debug output lines.
	 *
	 * @see Parser.Builder#debugOutputLines(int)
	 * @return
	 * 	The number of lines of input before and after the error location to be printed as part of the exception message.
	 */
	protected final int getDebugOutputLines() {
		return ctx.getDebugOutputLines();
	}

	/**
	 * Returns the listener associated with this session.
	 *
	 * @return The listener associated with this session, or <jk>null</jk> if there is no listener.
	 */
	public ParserListener getListener() {
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
		return ctx.isStrict();
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
		return ctx.isTrimStrings();
	}

	/**
	 * Unbuffered.
	 *
	 * @see Parser.Builder#unbuffered()
	 * @return
	 * 	<jk>true</jk> if parsers don't use internal buffering during parsing.
	 */
	protected final boolean isUnbuffered() {
		return ctx.isUnbuffered();
	}

	/**
	 * HTTP part schema of object being parsed.
	 *
	 * @return HTTP part schema of object being parsed, or <jk>null</jk> if not specified.
	 */
	public final HttpPartSchema getSchema() {
		return schema;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parser listener.
	 *
	 * @see Parser.Builder#listener(Class)
	 * @return
	 * 	Class used to listen for errors and warnings that occur during parsing.
	 */
	protected final Class<? extends ParserListener> getListenerClass() {
		return ctx.getListener();
	}

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("javaMethod", javaMethod, "listener", listener, "outer", outer);
	}
}
