/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.soap.*;
import com.ibm.juno.core.utils.*;

/**
 * Parent class for all Juno serializers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Base serializer class that serves as the parent class for all serializers.
 * <p>
 * 	Subclasses should extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer}.
 *
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * 	See {@link SerializerProperties} for a list of configurable properties that can be set on this class
 * 	using the {@link #setProperty(String, Object)} method.
 *
 * @param <W> The output stream or writer class type.
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class Serializer<W> extends CoreApi {

	/** General serializer properties currently set on this serializer. */
	protected transient SerializerProperties sp = new SerializerProperties();
	private String[] mediaTypes;
	private String contentType;

	// Hidden constructor to force subclass from OuputStreamSerializer or WriterSerializer.
	Serializer() {}

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
	 *
	 * @param o The object to serialize.
	 * @param out The writer or output stream to write to.
	 * @param ctx The serializer context object return by {@link #createContext(ObjectMap, Method)}.<br>
	 * 	If <jk>null</jk>, context is created using {@link #createContext()}.
	 *
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	protected abstract void doSerialize(Object o, W out, SerializerContext ctx) throws IOException, SerializeException;

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Calls {@link #serialize(Object, Object, SerializerContext)} but intercepts {@link StackOverflowError} exceptions
	 * 	and wraps them in a useful message.
	 * @param o The object to serialize.
	 * @param out The writer or output stream to write to.
	 * @param ctx The serializer context object return by {@link #createContext(ObjectMap, Method)}.<br>
	 * 	If <jk>null</jk>, context is created using {@link #createContext()}.
	 *
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final void serialize(Object o, W out, SerializerContext ctx) throws IOException, SerializeException {
		try {
			doSerialize(o, out, ctx);
		} catch (StackOverflowError e) {
			throw new SerializeException("Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the SerializerProperties.SERIALIZER_detectRecursions setting to help locate the loop.").initCause(e);
		} finally {
			ctx.close();
		}
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 * <p>
	 * Equivalent to calling <code>serializer.serialize(o, out, <jk>null</jk>);</code>
	 *
	 * @param o The object to serialize.
	 * @param out The writer or output stream to write to.
	 *
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final void serialize(Object o, W out) throws IOException, SerializeException {
		SerializerContext ctx = createContext();
		serialize(o, out, ctx);
	}

	/**
	 * Create the context object that will be passed in to the serialize method.
	 * <p>
	 * 	It's up to implementers to decide what the context object looks like, although typically
	 * 	it's going to be a subclass of {@link SerializerContext}.
	 *
	 * @param properties Optional additional properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 * @return The new context.
	 */
	public SerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new SerializerContext(getBeanContext(), sp, properties, javaMethod);
	}

	/**
	 * Create a basic context object without overriding properties or specifying <code>javaMethod</code>.
	 * <p>
	 * Equivalent to calling <code>createContext(<jk>null</jk>, <jk>null</jk>)</code>.
	 *
	 * @return The new context.
	 */
	protected SerializerContext createContext() {
		return createContext(null, null);
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
		if (mediaTypes == null) {
			Produces p = ReflectionUtils.getAnnotation(Produces.class, getClass());
			if (p == null)
				throw new RuntimeException(MessageFormat.format("Class ''{0}'' is missing the @Produces annotation", getClass().getName()));
			mediaTypes = p.value();
		}
		return mediaTypes;
	}

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 * <p>
	 * 	For example, {@link SoapXmlSerializer} needs to set a <code>SOAPAction</code> header.
	 * <p>
	 * 	This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @param properties Optional run-time properties (the same that are passed to {@link WriterSerializer#doSerialize(Object, Writer, SerializerContext)}.
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
	 * 	For example, the {@link com.ibm.juno.core.json.JsonSerializer.Simple} class returns that it handles media type <js>"text/json+simple"</js>, but returns
	 * 	<js>"text/json"</js> as the actual content type.
	 * 	This allows clients to request specific 'flavors' of content using specialized <code>Accept</code> header values.
	 * <p>
	 * 	This method is typically meaningless if the serializer is being used standalone (i.e. outside of a REST server or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public String getResponseContentType() {
		if (contentType == null) {
			Produces p = getClass().getAnnotation(Produces.class);
			if (p == null)
				contentType = "";
			else {
				contentType = p.contentType();
				if (contentType.isEmpty())
					contentType = p.value()[0];
			}
		}
		return (contentType.isEmpty() ? null : contentType);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public Serializer<W> setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! sp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public Serializer<W> addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public Serializer<W> addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> Serializer<W> addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public Serializer<W> setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* CoreApi */
	public Serializer<W> lock() {
		super.lock();
		return this;
	}

	@Override /* CoreApi */
	public Serializer<W> clone() throws CloneNotSupportedException {
		@SuppressWarnings("unchecked")
		Serializer<W> c = (Serializer<W>)super.clone();
		c.sp = sp.clone();
		return c;
	}
}
