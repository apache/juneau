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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	This class is typically the parent class of all character-based serializers.
 * 	It has 2 abstract methods to implement...
 * <ul class='spaced-list'>
 * 	<li>{@link #createSession(Object, ObjectMap, Method, Locale, TimeZone, MediaType)}
 * 	<li>{@link #doSerialize(SerializerSession, Object)}
 * </ul>
 *
 * <h6 class='topic'>@Produces annotation</h6>
 * <p>
 * 	The media types that this serializer can produce is specified through the {@link Produces @Produces} annotation.
 * <p>
 * 	However, the media types can also be specified programmatically by overriding the {@link #getMediaTypes()}
 * 		and {@link #getResponseContentType()} methods.
 */
public abstract class WriterSerializer extends Serializer {

	@Override /* Serializer */
	public boolean isWriterSerializer() {
		return true;
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Convenience method for serializing an object to a <code>String</code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override
	public final String serialize(Object o) throws SerializeException {
		StringWriter w = new StringWriter();
		serialize(createSession(w), o);
		return w.toString();
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
			serialize(createSession(w), o);
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

	/**
	 * Convenience method for serializing an object and sending it to STDOUT.
	 *
	 * @param o The object to serialize.
	 * @return This object (for method chaining).
	 */
	public final WriterSerializer println(Object o) {
		System.out.println(toString(o));
		return this;
	}
}
