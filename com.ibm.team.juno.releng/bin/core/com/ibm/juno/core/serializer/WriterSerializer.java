/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This class is typically the parent class of all character-based serializers.
 * 	It has 2 abstract methods to implement...
 * <ul>
 * 	<li>{@link #createContext(ObjectMap, Method)}
 * 	<li>{@link #doSerialize(Object, Writer, SerializerContext)}
 * </ul>
 *
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class WriterSerializer extends Serializer<Writer> {

	@Override /* Serializer */
	public boolean isWriterSerializer() {
		return true;
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected abstract void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException;


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Internal test method.
	 *
	 * @param o The object to serialize.
	 * @param ctx The serialize context.
	 * 	This object is automatically closed after serialization.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final String serialize(Object o, SerializerContext ctx) throws SerializeException {
		try {
			StringWriter w = new StringWriter();
			serialize(o, w, ctx);
			return w.toString();
		} catch (IOException e) { // Shouldn't happen.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convenience method for serializing an object to a String.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final String serialize(Object o) throws SerializeException {
		try {
			StringWriter w = new StringWriter();
			serialize(o, w, createContext());
			return w.toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}

	/**
	 * Identical to {@link #serialize(Object)} except throws a {@link RuntimeException}
	 * instead of a {@link SerializeException}.
	 * This is typically good enough for debugging purposes.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public final String toString(Object o) {
		try {
			StringWriter w = new StringWriter();
			serialize(o, w, createContext());
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wraps the specified object inside a {@link StringObject}.
	 *
	 * @param o The object to wrap.
	 * @return The wrapped object.
	 */
	public final StringObject toStringObject(Object o) {
		return new StringObject(this, o);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public WriterSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public WriterSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> WriterSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public WriterSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public WriterSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* CoreApi */
	public WriterSerializer clone() throws CloneNotSupportedException {
		WriterSerializer c = (WriterSerializer)super.clone();
		return c;
	}
}
