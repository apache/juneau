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
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Base serializer class that serves as the parent class for all serializers.
 * <p>
 * 	Subclasses should extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer}.
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 *
 * <h6 class='topic'>Configurable properties</h6>
 * 	See {@link SerializerContext} for a list of configurable properties that can be set on this class
 * 	using the {@link #setProperty(String, Object)} method.
 */
public abstract class Serializer extends CoreApi {

	private final String[] mediaTypes;
	private final MediaRange[] mediaRanges;
	private final String contentType;

	// Hidden constructors to force subclass from OuputStreamSerializer or WriterSerializer.
	Serializer() {
		Produces p = ReflectionUtils.getAnnotation(Produces.class, getClass());
		if (p == null)
			throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Produces annotation", getClass().getName()));
		this.mediaTypes = StringUtils.split(p.value(), ',');
		for (int i = 0; i < mediaTypes.length; i++) {
			mediaTypes[i] = mediaTypes[i].toLowerCase(Locale.ENGLISH);
		}

		List<MediaRange> l = new LinkedList<MediaRange>();
		for (int i = 0; i < mediaTypes.length; i++)
			l.addAll(Arrays.asList(MediaRange.parse(mediaTypes[i])));
		mediaRanges = l.toArray(new MediaRange[l.size()]);

		String ct = p.contentType().isEmpty() ? this.mediaTypes[0] : p.contentType();
		contentType = ct.isEmpty() ? null : ct;
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
	 * @param session The serializer session object return by {@link #createSession(Object, ObjectMap, Method)}.<br>
	 * 	If <jk>null</jk>, session is created using {@link #createSession(Object)}.
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
	 * @param session The serializer session object return by {@link #createSession(Object, ObjectMap, Method)}.<br>
	 * 	If <jk>null</jk>, session is created using {@link #createSession(Object)}.
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
	 * 	It's up to implementers to decide what the session object looks like, although typically
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
	 * @param properties Optional additional properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 * @return The new session.
	 */
	public SerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new SerializerSession(getContext(SerializerContext.class), getBeanContext(), output, properties, javaMethod);
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
		return createSession(output, null, null);
	}

	/**
	 * Converts the contents of the specified object array to a list.
	 * <p>
	 * 	Works on both object and primitive arrays.
	 * <p>
	 * 	In the case of multi-dimensional arrays, the outgoing list will
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

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 * <p>
	 * 	For example, {@link SoapXmlSerializer} needs to set a <code>SOAPAction</code> header.
	 * <p>
	 * 	This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @param properties Optional run-time properties (the same that are passed to {@link WriterSerializer#doSerialize(SerializerSession, Object)}.
	 * 	Can be <jk>null</jk>.
	 * @return The HTTP headers to set on HTTP requests.
	 * 	Can be <jk>null</jk>.
	 */
	public ObjectMap getResponseHeaders(ObjectMap properties) {
		return new ObjectMap(getBeanContext());
	}

	/**
	 * Optional method that returns the response <code>Content-Type</code> for this serializer if it is different from the matched media type.
	 * <p>
	 * 	This method is specified to override the content type for this serializer.
	 * 	For example, the {@link org.apache.juneau.json.JsonSerializer.Simple} class returns that it handles media type <js>"text/json+simple"</js>, but returns
	 * 	<js>"text/json"</js> as the actual content type.
	 * 	This allows clients to request specific 'flavors' of content using specialized <code>Accept</code> header values.
	 * <p>
	 * 	This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public String getResponseContentType() {
		return contentType;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public Serializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public Serializer addToDictionary(Class<?>...classes) throws LockedException {
		super.addToDictionary(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> Serializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

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
