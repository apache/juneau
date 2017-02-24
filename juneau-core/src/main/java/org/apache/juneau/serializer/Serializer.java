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
package org.apache.juneau.serializer;

import static org.apache.juneau.serializer.SerializerContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.soap.*;

/**
 * Parent class for all Juneau serializers.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Base serializer class that serves as the parent class for all serializers.
 * <p>
 * Subclasses should extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer}.
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 	and {@link #getResponseContentType()} methods.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * See {@link SerializerContext} for a list of configurable properties that can be set on this class
 * 	using the {@link #setProperty(String, Object)} method.
 */
public abstract class Serializer extends CoreApi {

	private final MediaType[] mediaTypes;
	private final MediaType contentType;

	// Hidden constructors to force subclass from OuputStreamSerializer or WriterSerializer.
	Serializer() {
		Produces p = ReflectionUtils.getAnnotation(Produces.class, getClass());
		if (p == null)
			throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Produces annotation", getClass().getName()));

		String[] mt = StringUtils.split(p.value(), ',');
		this.mediaTypes = new MediaType[mt.length];
		for (int i = 0; i < mt.length; i++) {
			mediaTypes[i] = MediaType.forString(mt[i]);
		}

		String ct = p.contentType().isEmpty() ? this.mediaTypes[0].toString() : p.contentType();
		contentType = ct.isEmpty() ? null : MediaType.forString(ct);
	}

	/**
	 * Returns <jk>true</jk> if this parser subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this parser subclasses from {@link WriterSerializer}.
	 */
	public abstract boolean isWriterSerializer();

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 * <p>
	 * This method should NOT close the context object.
	 * @param session The serializer session object return by {@link #createSession(Object, ObjectMap, Method, Locale, TimeZone, MediaType)}.<br>
	 * If <jk>null</jk>, session is created using {@link #createSession(Object)}.
	 * @param o The object to serialize.
	 *
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected abstract void doSerialize(SerializerSession session, Object o) throws Exception;

	/**
	 * Shortcut method for serializing objects directly to either a <code>String</code> or <code><jk>byte</jk>[]</code>
	 * 	depending on the serializer type.
	 * <p>
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 * 	<br>Character-based serializers will return a <code>String</code>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public abstract Object serialize(Object o) throws SerializeException;

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Serialize the specified object using the specified session.
	 *
	 * @param session The serializer session object return by {@link #createSession(Object, ObjectMap, Method, Locale, TimeZone, MediaType)}.<br>
	 * If <jk>null</jk>, session is created using {@link #createSession(Object)}.
	 * @param o The object to serialize.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final void serialize(SerializerSession session, Object o) throws SerializeException {
		try {
			doSerialize(session, o);
		} catch (SerializeException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new SerializeException(session, "Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the SerializerContext.SERIALIZER_detectRecursions setting to help locate the loop.").initCause(e);
		} catch (Exception e) {
			throw new SerializeException(session, e);
		} finally {
			session.close();
		}
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 * <p>
	 * Equivalent to calling <code>serializer.serialize(o, out, <jk>null</jk>);</code>
	 *
	 * @param o The object to serialize.
	 * @param output The output object.
	 * 	<br>Character-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 	</ul>
	 * 	<br>Stream-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final void serialize(Object o, Object output) throws SerializeException {
		SerializerSession session = createSession(output);
		serialize(session, o);
	}

	/**
	 * Create the session object that will be passed in to the serialize method.
	 * <p>
	 * It's up to implementers to decide what the session object looks like, although typically
	 * 	it's going to be a subclass of {@link SerializerSession}.
	 *
	 * @param output The output object.
	 * 	<br>Character-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 	</ul>
	 * 	<br>Stream-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param op Optional additional properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @return The new session.
	 */
	public SerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new SerializerSession(getContext(SerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	/**
	 * Create a basic session object without overriding properties or specifying <code>javaMethod</code>.
	 * <p>
	 * Equivalent to calling <code>createSession(<jk>null</jk>, <jk>null</jk>)</code>.
	 *
	 * @param output The output object.
	 * 	<br>Character-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 	</ul>
	 * 	<br>Stream-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @return The new session.
	 */
	protected SerializerSession createSession(Object output) {
		return createSession(output, null, null, null, null, getPrimaryMediaType());
	}

	/**
	 * Converts the contents of the specified object array to a list.
	 * <p>
	 * Works on both object and primitive arrays.
	 * <p>
	 * In the case of multi-dimensional arrays, the outgoing list will
	 * 	contain elements of type n-1 dimension.  i.e. if {@code type} is <code><jk>int</jk>[][]</code>
	 * 	then {@code list} will have entries of type <code><jk>int</jk>[]</code>.
	 *
	 * @param type The type of array.
	 * @param array The array being converted.
	 * @return The array as a list.
	 */
	protected final List<Object> toList(Class<?> type, Object array) {
		Class<?> componentType = type.getComponentType();
		if (componentType.isPrimitive()) {
			int l = Array.getLength(array);
			List<Object> list = new ArrayList<Object>(l);
			for (int i = 0; i < l; i++)
				list.add(Array.get(array, i));
			return list;
		}
		return Arrays.asList((Object[])array);
	}

	/**
	 * Returns the media types handled based on the value of the {@link Produces} annotation on the serializer class.
	 * <p>
	 * This method can be overridden by subclasses to determine the media types programatically.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public MediaType[] getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * Returns the first media type specified on this parser via the {@link Produces} annotation.
	 *
	 * @return The media type.
	 */
	public MediaType getPrimaryMediaType() {
		return mediaTypes == null || mediaTypes.length == 0 ? null : mediaTypes[0];
	}

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 * <p>
	 * For example, {@link SoapXmlSerializer} needs to set a <code>SOAPAction</code> header.
	 * <p>
	 * This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @param properties Optional run-time properties (the same that are passed to {@link WriterSerializer#doSerialize(SerializerSession, Object)}.
	 * Can be <jk>null</jk>.
	 * @return The HTTP headers to set on HTTP requests.
	 * Can be <jk>null</jk>.
	 */
	public ObjectMap getResponseHeaders(ObjectMap properties) {
		return ObjectMap.EMPTY_MAP;
	}

	/**
	 * Optional method that returns the response <code>Content-Type</code> for this serializer if it is different from the matched media type.
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.JsonSerializer.Simple} class returns that it handles media type <js>"text/json+simple"</js>, but returns
	 * 	<js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <code>Accept</code> header values.
	 * <p>
	 * This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public MediaType getResponseContentType() {
		return contentType;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Max serialization depth.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.maxDepth"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>100</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 * This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_maxDepth</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_maxDepth
	 */
	public Serializer setMaxDepth(int value) throws LockedException {
		return setProperty(SERIALIZER_maxDepth, value);
	}

	/**
	 * <b>Configuration property:</b>  Initial depth.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.initialDepth"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>0</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The initial indentation level at the root.
	 * Useful when constructing document fragments that need to be indented at a certain level.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_initialDepth</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_initialDepth
	 */
	public Serializer setInitialDepth(int value) throws LockedException {
		return setProperty(SERIALIZER_initialDepth, value);
	}

	/**
	 * <b>Configuration property:</b>  Automatically detect POJO recursions.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.detectRecursions"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 * <p>
	 * Recursions can occur when serializing models that aren't true trees, but rather contain loops.
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link SerializerContext#SERIALIZER_ignoreRecursions}.
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>SERIALIZER_ignoreRecursions</jsf> is <jk>true</jk>...
	 * <code>{A:{B:{C:null}}}</code><br>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_detectRecursions</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_detectRecursions
	 */
	public Serializer setDetectRecursions(boolean value) throws LockedException {
		return setProperty(SERIALIZER_detectRecursions, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore recursion errors.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.ignoreRecursions"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Used in conjunction with {@link SerializerContext#SERIALIZER_detectRecursions}.
	 * Setting is ignored if <jsf>SERIALIZER_detectRecursions</jsf> is <jk>false</jk>.
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree,
	 * 	we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_ignoreRecursions</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_ignoreRecursions
	 */
	public Serializer setIgnoreRecursions(boolean value) throws LockedException {
		return setProperty(SERIALIZER_ignoreRecursions, value);
	}

	/**
	 * <b>Configuration property:</b>  Use whitespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.useWhitepace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, newlines and indentation and spaces are added to the output to improve readability.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_useWhitespace</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_useWhitespace
	 */
	public Serializer setUseWhitespace(boolean value) throws LockedException {
		return setProperty(SERIALIZER_useWhitespace, value);
	}

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from the value type.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_addBeanTypeProperties</jsf>, value)</code>.
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_addBeanTypeProperties
	 */
	public Serializer setAddBeanTypeProperties(boolean value) throws LockedException {
		return setProperty(SERIALIZER_addBeanTypeProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Quote character.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.quoteChar"</js>
	 * 	<li><b>Data type:</b> <code>Character</code>
	 * 	<li><b>Default:</b> <js>'"'</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * This is the character used for quoting attributes and values.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_quoteChar</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_quoteChar
	 */
	public Serializer setQuoteChar(char value) throws LockedException {
		return setProperty(SERIALIZER_quoteChar, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim null bean property values.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimNullProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_trimNullProperties</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with <jk>null</jk> values will be lost.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimNullProperties
	 */
	public Serializer setTrimNullProperties(boolean value) throws LockedException {
		return setProperty(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim empty lists and arrays.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyLists"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_trimEmptyCollections</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with empty list values will be lost.
	 * 		<li>Bean properties with empty list values will not be set.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimEmptyCollections
	 */
	public Serializer setTrimEmptyCollections(boolean value) throws LockedException {
		return setProperty(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim empty maps.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyMaps"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_trimEmptyMaps</jsf>, value)</code>.
	 * 	<li>Enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Bean properties with empty map values will not be set.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimEmptyMaps
	 */
	public Serializer setTrimEmptyMaps(boolean value) throws LockedException {
		return setProperty(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * <b>Configuration property:</b>  Trim strings.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_trimStrings</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_trimStrings
	 */
	public Serializer setTrimStrings(boolean value) throws LockedException {
		return setProperty(SERIALIZER_trimStrings, value);
	}

	/**
	 * <b>Configuration property:</b>  URI base for relative URIs.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.relativeUriBase"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Prepended to relative URIs during serialization (along with the {@link SerializerContext#SERIALIZER_absolutePathUriBase} if specified.
	 * (i.e. URIs not containing a schema and not starting with <js>'/'</js>).
	 * (e.g. <js>"foo/bar"</js>)
	 *
	 * <h5 class='section'>Example:</h5>
	 * <table class='styled'>
	 * 	<tr><th>SERIALIZER_relativeUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 		<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 	</tr>
	 * </table>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_relativeUriBase</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_relativeUriBase
	 */
	public Serializer setRelativeUriBase(String value) throws LockedException {
		return setProperty(SERIALIZER_relativeUriBase, value);
	}

	/**
	 * <b>Configuration property:</b>  URI base for relative URIs with absolute paths.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.absolutePathUriBase"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Prepended to relative absolute-path URIs during serialization.
	 * (i.e. URIs starting with <js>'/'</js>).
	 * (e.g. <js>"/foo/bar"</js>)
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <table class='styled'>
	 * 	<tr><th>SERIALIZER_absolutePathUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 		<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 	</tr>
	 * </table>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_absolutePathUriBase</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_absolutePathUriBase
	 */
	public Serializer setAbsolutePathUriBase(String value) throws LockedException {
		return setProperty(SERIALIZER_absolutePathUriBase, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort arrays and collections alphabetically.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortCollections"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_sortCollections</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_sortCollections
	 */
	public Serializer setSortCollections(boolean value) throws LockedException {
		return setProperty(SERIALIZER_sortCollections, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort maps alphabetically.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortMaps"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>SERIALIZER_sortMaps</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public Serializer setSortMaps(boolean value) throws LockedException {
		return setProperty(SERIALIZER_sortMaps, value);
	}

	@Override /* CoreApi */
	public Serializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public Serializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* CoreApi */
	public Serializer lock() {
		super.lock();
		return this;
	}

	@Override /* CoreApi */
	public Serializer clone() throws CloneNotSupportedException {
		Serializer c = (Serializer)super.clone();
		return c;
	}
}
